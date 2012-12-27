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

import org.jmxexporter.output.OutputWriter;
import org.jmxexporter.util.Preconditions;
import org.jmxexporter.util.concurrent.DiscardingBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Describe a JMX query on which metrics are collected and hold the query and export business logic
 * ({@link #collectMetrics(javax.management.MBeanServer)} and {@link #exportCollectedMetrics()}).
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class Query {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Parent.
     */
    private JmxExporter jmxExporter;

    /**
     * ObjectName of the Query MBean(s) to monitor, can contain
     */
    @Nonnull
    private ObjectName objectName;

    @Nullable
    private String resultAlias;
    /**
     * JMX attributes to collect. As an array for {@link javax.management.MBeanServer#getAttributes(javax.management.ObjectName, String[])}
     */
    @Nonnull
    private Map<String, QueryAttribute> attributesByName = new HashMap<String, QueryAttribute>();
    /**
     * Copy of {@link #attributesByName}'s {@link java.util.Map#entrySet()} for performance optimization
     */
    @Nonnull
    private String[] attributeNames = new String[0];

    /**
     * List of {@linkplain OutputWriter} declared at the {@linkplain Query} level.
     *
     * @see org.jmxexporter.JmxExporter#getOutputWriters()
     * @see #getEffectiveOutputWriters()
     */
    @Nonnull
    private List<OutputWriter> outputWriters = new ArrayList<OutputWriter>();

    /**
     * Store the metrics collected on this {@linkplain Query} (see {@link #collectMetrics(javax.management.MBeanServer)})
     * until they are exported to the target {@linkplain OutputWriter}s (see {@link #exportCollectedMetrics()}.
     */
    @Nonnull
    private BlockingQueue<QueryResult> queryResults = new DiscardingBlockingQueue<QueryResult>(200);

    /**
     * @param objectName {@link ObjectName} to query, can contain wildcards ('*' or '?')
     */
    public Query(String objectName) {
        try {
            this.objectName = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Exception parsing '" + objectName + "'", e);
        }
    }

    public Query(ObjectName objectName) {
        this.objectName = objectName;
    }


    public void collectMetrics(MBeanServer mbeanServer) {
        /*
         * Optimisation tip: no need to skip 'mbeanServer.queryNames()' if the ObjectName is not a pattern
         * (i.e. not '*' or '?' wildcard) because the mbeanserver internally performs the check.
         * Seen on com.sun.jmx.interceptor.DefaultMBeanServerInterceptor
         */
        Set<ObjectName> matchingObjectNames = mbeanServer.queryNames(this.objectName, null);

        for (ObjectName matchingObjectName : matchingObjectNames) {
            long epochInMillis = System.currentTimeMillis();
            try {
                AttributeList jmxAttributes = mbeanServer.getAttributes(matchingObjectName, this.attributeNames);
                for (Attribute jmxAttribute : jmxAttributes.asList()) {
                    QueryAttribute queryAttribute = this.attributesByName.get(jmxAttribute.getName());
                    Object value = jmxAttribute.getValue();
                    queryAttribute.performQuery(matchingObjectName, value, epochInMillis, this.queryResults);
                }
            } catch (Exception e) {
                logger.warn("Exception processing query {}", this, e);
            }
        }
    }

    /**
     * Export the collected metrics to the {@linkplain OutputWriter}s associated with this {@linkplain Query}
     * (see {@link #getEffectiveOutputWriters()}).
     * <p/>
     * Metrics are batched according to {@link org.jmxexporter.JmxExporter#getExportBatchSize()}
     *
     * @return the number of exported {@linkplain QueryResult}
     */
    public int exportCollectedMetrics() {

        int resultCount = 0;

        List<OutputWriter> effectiveOutputWriters = getEffectiveOutputWriters();
        int exportBatchSize = getJmxExporter().getExportBatchSize();
        List<QueryResult> availableQueryResults = new ArrayList<QueryResult>(exportBatchSize);

        while (queryResults.drainTo(availableQueryResults, exportBatchSize) > 0) {
            resultCount += availableQueryResults.size();
            for (OutputWriter outputWriter : effectiveOutputWriters) {
                outputWriter.write(availableQueryResults);
            }
            availableQueryResults.clear();
        }
        return resultCount;
    }

    /**
     * Start all the {@linkplain OutputWriter}s attached to this {@linkplain Query}
     */
    @PostConstruct
    public void start() throws Exception {
        for (OutputWriter outputWriter : outputWriters) {
            outputWriter.start();
        }
    }

    /**
     * Stop all the {@linkplain OutputWriter}s attached to this {@linkplain Query}
     */
    @PreDestroy
    public void stop() throws Exception {
        for (OutputWriter outputWriter : outputWriters) {
            outputWriter.stop();
        }
    }

    @Nonnull
    public ObjectName getObjectName() {
        return objectName;
    }

    @Nonnull
    public Collection<QueryAttribute> getQueryAttributes() {
        return attributesByName.values();
    }

    /**
     * Add the given attribute to the list attributes of this query
     * and maintains the reverse relation (see {@link org.jmxexporter.QueryAttribute#getQuery()}).
     *
     * @param attribute attribute to add
     * @return this
     */
    @Nonnull
    public Query addAttribute(@Nonnull QueryAttribute attribute) {
        attribute.setQuery(this);
        attributesByName.put(attribute.getName(), attribute);
        attributeNames = attributesByName.keySet().toArray(new String[0]);

        return this;
    }

    /**
     * Create a basic {@link QueryAttribute}, add it to the list attributes of this query
     * and maintains the reverse relation (see {@link org.jmxexporter.QueryAttribute#getQuery()}).
     *
     * @param attributeName attribute to add
     * @return this
     */
    @Nonnull
    public Query addAttribute(@Nonnull String attributeName) {
        return addAttribute(new QueryAttribute(attributeName, null));
    }

    @Nonnull
    public BlockingQueue<QueryResult> getResults() {
        return queryResults;
    }

    /**
     * WARNING: {@linkplain #queryResults} queue should not be changed at runtime as the operation is not thread safe.
     *
     * @param queryResultQueue
     */
    public void setResultsQueue(@Nonnull BlockingQueue<QueryResult> queryResultQueue) {
        this.queryResults = Preconditions.checkNotNull(queryResultQueue);
    }

    public void setResultAlias(@Nullable String resultAlias) {
        this.resultAlias = resultAlias;
    }

    public JmxExporter getJmxExporter() {
        return jmxExporter;
    }

    public void setJmxExporter(JmxExporter jmxExporter) {
        this.jmxExporter = jmxExporter;
    }

    /**
     * Return the <code>outputWriters</code> to which the collected metrics of this {@linkplain Query} are exported,
     * the <code>outputWriters</code> declared at query level or a the parent level.
     *
     * @see #getOutputWriters()
     * @see JmxExporter#getOutputWriters()
     */
    @Nonnull
    public List<OutputWriter> getEffectiveOutputWriters() {
        List<OutputWriter> result = new ArrayList<OutputWriter>(jmxExporter.getOutputWriters().size() + outputWriters.size());
        result.addAll(jmxExporter.getOutputWriters());
        result.addAll(outputWriters);
        return result;
    }

    @Nonnull
    public List<OutputWriter> getOutputWriters() {
        return outputWriters;
    }

    @Nullable
    public String getResultAlias() {
        return resultAlias;
    }

    @Override
    public String toString() {
        return "Query{" +
                "objectName=" + objectName +
                ", resultAlias='" + resultAlias + '\'' +
                ", outputWriters=" + outputWriters +
                ", attributes=" + attributesByName.values() +
                '}';
    }
}
