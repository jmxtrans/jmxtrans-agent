/*
 * Copyright 2008-2012 Xebia and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmxexporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.*;

/**
 * Query attribute descriptor for JMX {@linkplain javax.management.openmbean.CompositeData}.
 */
public class QueryCompositeAttribute extends AbstractQueryAttribute implements QueryAttribute {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected ResultNameStrategy resultNameStrategy = new ResultNameStrategy();

    /**
     * @see javax.management.openmbean.CompositeType#keySet()
     */
    private String[] keys = new String[0];

    /**
     * @param name  name of the JMX attribute
     * @param alias
     * @param keys
     */
    public QueryCompositeAttribute(String name, String alias, Collection<String> keys) {
        super(name, alias);
        addKeys(keys);
    }

    public void addKeys(Collection<String> keys) {
        Set<String> newKeys = new HashSet<String>(keys);
        Collections.addAll(newKeys, this.keys);
        this.keys = newKeys.toArray(new String[0]);
    }

    @Override
    public void performQuery(ObjectName objectName, Object value, long epochInMillis, Queue<QueryResult> results) {
        if (value instanceof CompositeData) {
            CompositeData compositeData = (CompositeData) value;
            for (String key : keys) {
                String resultName = resultNameStrategy.getResultName(getQuery(), objectName, this, key);
                Object compositeValue = compositeData.get(key);
                if (compositeValue instanceof Number || compositeValue instanceof String || compositeValue instanceof Date) {
                    QueryResult result = new QueryResult(resultName, compositeValue, epochInMillis);
                    logger.debug("Collect {}", result);
                    results.add(result);
                } else {
                    logger.trace("Skip non supported value {}:{}:{}:{}={}", getQuery(), objectName, this, key, compositeValue);
                }
            }
        } else if (value instanceof Number || value instanceof String || value instanceof Date) {
            logger.info("Unexpected 'simple' value for {}:{}:{}", getQuery(), objectName, this);
            String resultName = resultNameStrategy.getResultName(getQuery(), objectName, this);
            QueryResult result = new QueryResult(resultName, value, epochInMillis);
            logger.debug("Collect {}", result);
            results.add(result);
        } else {
            logger.info("Ignore non CompositeData attribute value {}:{}:{}={}", getQuery(), objectName, this, value);
        }
    }

    @Override
    public String toString() {
        return "QueryCompositeAttribute{" +
                "name='" + getName() + '\'' +
                ", resultAlias='" + getResultAlias() + '\'' +
                ", keys=" + (keys == null ? null : Arrays.asList(keys)) +
                '}';
    }
}
