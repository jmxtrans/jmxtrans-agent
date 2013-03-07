/*
 * Copyright (c) 2010-2013 the original author or authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package org.jmxtrans.embedded;

import org.jmxtrans.embedded.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.*;

/**
 * Describe a JMX MBean attribute to collect and hold the attribute collection logic.
 * <p/>
 * Collected values are sent to a {@linkplain java.util.concurrent.BlockingQueue}
 * for later export to the target monitoring systems
 * (see {@link #collectMetrics(javax.management.ObjectName, Object, long, java.util.Queue)}).
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 * @author Jon Stevens
 */
public class QueryAttribute {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ResultNameStrategy resultNameStrategy = new ResultNameStrategy();

    private Query query;
    /**
     * Name of the JMX Attribute to collect
     */
    @Nonnull
    private String name;
    /**
     * Used to build the name of the {@link QueryResult} that will be exported.
     * <p/>
     * <code>null</code> if not defined in the configuration. The {@link #name} is then used to build to {@linkplain QueryResult}
     *
     * @see org.jmxtrans.embedded.QueryResult#getName()
     */
    @Nullable
    private String resultAlias;

    /**
     * Attribute type like '{@code gauge}' or '{@code counter}'. Used by monitoring systems like Librato who require this information.
     *
     * @see org.jmxtrans.embedded.QueryResult#getName()
     */
    @Nullable
    private String type;

    /**
     * <code>null</code> if no 'key' as been defined in the config.
     * Empty list if empty 'key' node has been declared in the config.
     *
     * @see javax.management.openmbean.CompositeType#keySet()
     */
    @Nullable
    private String[] keys;

    /**
     * @param name        name of the JMX attribute
     * @param type        type of the metric (e.g. "{@code counter}", "{@code gauge}", ...)
     * @param resultAlias name of the result that will be exported
     */
    public QueryAttribute(@Nonnull String name, @Nullable String type, @Nullable String resultAlias) {
        this.name = Preconditions.checkNotEmpty(name);
        this.type = type;
        this.resultAlias = resultAlias;
    }

    /**
     * @param name        name of the JMX attribute
     * @param type        type of the metric (e.g. "{@code counter}", "{@code gauge}", ...)
     * @param resultAlias name of the result that will be exported
     * @param keys        of the {@link CompositeData} to collect
     */
    public QueryAttribute(@Nonnull String name, @Nullable String type, @Nullable String resultAlias, @Nullable Collection<String> keys) {
        this(name, type, resultAlias);
        addKeys(keys);
    }

    /**
     * Not <code>null</code> once this {@link QueryAttribute} has been added to its parent {@link Query}.
     *
     * @return parent query
     * @see Query#addAttribute(QueryAttribute)
     */
    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getResultAlias() {
        return resultAlias;
    }

    @Nullable
    public String getType() {
        return type;
    }

    /**
     * @param objectName    <code>objectName</code> on which the <code>attribute</code> was obtained.
     * @param value         value of the given attribute. A 'simple' value (String, Number, Date)
     *                      or a {@link javax.management.openmbean.CompositeData}
     * @param epochInMillis time at which the metric was collected
     * @param results       queue to which the the computed result(s) must be added
     * @return collected metrics count
     */
    public int collectMetrics(@Nonnull ObjectName objectName, @Nonnull Object value, long epochInMillis,
                              @Nonnull Queue<QueryResult> results) {

        int metricsCounter = 0;

        if (value instanceof CompositeData) {
            CompositeData compositeData = (CompositeData) value;
            String[] keysToCollect;
            if (keys == null) {
                keysToCollect = compositeData.getCompositeType().keySet().toArray(new String[0]);
                logger.info("No 'key' has been configured to collect data on this Composite attribute, collect all keys. {}:{}:{}", getQuery(), objectName, this);
            } else {
                keysToCollect = keys;
            }
            for (String key : keysToCollect) {
                String resultName = resultNameStrategy.getResultName(getQuery(), objectName, this, key);
                Object compositeValue = compositeData.get(key);
                if (compositeValue instanceof Number || compositeValue instanceof String || compositeValue instanceof Date) {
                    QueryResult result = new QueryResult(resultName, compositeValue, epochInMillis);
                    logger.debug("Collect {}", result);
                    results.add(result);
                    metricsCounter++;
                } else {
                    logger.debug("Skip non supported value {}:{}:{}:{}={}", getQuery(), objectName, this, key, compositeValue);
                }
            }
        } else if (value instanceof Number || value instanceof String || value instanceof Date) {
            if (keys != null && logger.isInfoEnabled()) {
                logger.info("Ignore keys configured for 'simple' jmx attribute. {}:{}:{}", getQuery(), objectName, this);
            }
            String resultName = resultNameStrategy.getResultName(getQuery(), objectName, this);
            QueryResult result = new QueryResult(resultName, value, epochInMillis);
            logger.debug("Collect {}", result);
            results.add(result);
            metricsCounter++;
        } else {
            logger.info("Ignore non CompositeData attribute value {}:{}:{}={}", getQuery(), objectName, this, value);
        }
        return metricsCounter;
    }

    /**
     * @return <code>this</code>
     */
    @Nonnull
    public QueryAttribute addKeys(@Nullable Collection<String> newKeys) {
        if (newKeys == null) {
            return this;
        }

        Set<String> newKeysSet = new HashSet<String>(newKeys);
        if (this.keys != null) {
            Collections.addAll(newKeysSet, this.keys);
        }
        this.keys = newKeysSet.toArray(new String[0]);
        return this;
    }

    @Override
    public String toString() {
        return "QueryAttribute{" +
                "name='" + getName() + '\'' +
                ", resultAlias='" + getResultAlias() + '\'' +
                ", keys=" + (keys == null ? null : Arrays.asList(keys)) +
                '}';
    }
}
