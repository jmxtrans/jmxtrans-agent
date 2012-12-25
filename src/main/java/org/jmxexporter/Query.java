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
import org.jmxexporter.util.concurrent.DiscardingBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class Query {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JmxExporter jmxExporter;
    /**
     * ObjectName of the Query MBean(s) to monitor, can contain
     */
    private ObjectName objectName;

    private String resultAlias;
    /**
     * JMX attributes to collect. As an array for {@link javax.management.MBeanServer#getAttributes(javax.management.ObjectName, String[])}
     */
    private Map<String, QueryAttribute> attributesByName = new HashMap<String, QueryAttribute>();
    private String[] attributeNames = new String[0];

    private List<OutputWriter> outputWriters = new ArrayList<OutputWriter>();

    private BlockingQueue<QueryResult> queryResults = new DiscardingBlockingQueue<QueryResult>(200);

    public Query() {
    }

    public Query(String objectName) {
        this();
        try {
            this.objectName = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Exception parsing '" + objectName + "'", e);
        }
    }

    public Query(ObjectName objectName) {
        this();
        this.objectName = objectName;
    }


    public void performQuery(MBeanServer mbeanServer) {
        Set<ObjectName> matchingObjectNames = mbeanServer.queryNames(getObjectName(), null);
        for (ObjectName matchingObjectName : matchingObjectNames) {
            long epoch = System.currentTimeMillis();
            try {
                AttributeList jmxAttributes = mbeanServer.getAttributes(matchingObjectName, getAttributeNames());
                for (Attribute jmxAttribute : jmxAttributes.asList()) {
                    QueryAttribute queryAttribute = getAttribute(jmxAttribute.getName());
                    Collection<QueryResult> attributeResults = queryAttribute.parseAttribute(matchingObjectName, jmxAttribute, epoch);
                    addResults(attributeResults);
                }
            } catch (Exception e) {
                logger.warn("Exception processing query {}", this, e);
            }
        }
    }

    /**
     * Return the number of exported {@linkplain QueryResult}
     */
    public int performExport() {

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

    @PostConstruct
    public void start() throws Exception {
        for (OutputWriter outputWriter : outputWriters) {
            outputWriter.start();
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        for (OutputWriter outputWriter : outputWriters) {
            outputWriter.stop();
        }
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public void addResults(Iterable<QueryResult> results) {
        for (QueryResult result : results) {
            addResult(result);
        }
    }

    public void addResult(QueryResult queryResult) {
        queryResult.setQuery(this);
        queryResults.add(queryResult);
    }

    public Collection<QueryAttribute> getQueryAttributes() {
        return attributesByName.values();
    }

    public Query addAttribute(QueryAttribute attribute) {
        attribute.setQuery(this);
        attributesByName.put(attribute.getName(), attribute);
        attributeNames = attributesByName.keySet().toArray(new String[0]);

        return this;
    }

    public Query addAttribute(String attributeName) {
        return addAttribute(new QueryAttribute(attributeName));
    }

    public String[] getAttributeNames() {
        return attributeNames;
    }

    public QueryAttribute getAttribute(String name) {
        return attributesByName.get(name);
    }

    public BlockingQueue<QueryResult> getResults() {
        return queryResults;
    }

    /**
     * WARNING: {@linkplain #queryResults} queue should not be changed at runtime as the operation is not thread safe.
     *
     * @param queryResultQueue
     */
    protected void setResultsQueue(BlockingQueue<QueryResult> queryResultQueue) {
        this.queryResults = queryResultQueue;
    }

    public String escapeObjectName() {
        return getObjectName().getCanonicalKeyPropertyListString();
    }

    public String getResultName() {
        return resultAlias == null ? escapeObjectName() : resultAlias;
    }

    public void setResultAlias(String resultAlias) {
        this.resultAlias = resultAlias;
    }

    public JmxExporter getJmxExporter() {
        return jmxExporter;
    }

    public void setJmxExporter(JmxExporter jmxExporter) {
        this.jmxExporter = jmxExporter;
    }

    /**
     * Return <code>outputWriters</code> declared at query level or a the parent level.
     *
     * @see #getOutputWriters()
     * @see JmxExporter#getOutputWriters()
     */
    public List<OutputWriter> getEffectiveOutputWriters() {
        List<OutputWriter> result = new ArrayList<OutputWriter>(jmxExporter.getOutputWriters().size() + outputWriters.size());
        result.addAll(jmxExporter.getOutputWriters());
        result.addAll(outputWriters);
        return result;
    }

    public List<OutputWriter> getOutputWriters() {
        return outputWriters;
    }

    @Override
    public String toString() {
        return "Query{" +
                "objectName=" + objectName +
                ", resultAlias='" + resultAlias + '\'' +
                ", outputWriters=" + outputWriters +
                ", attributes=" + (attributesByName == null ? null : attributesByName.values()) +
                '}';
    }
}
