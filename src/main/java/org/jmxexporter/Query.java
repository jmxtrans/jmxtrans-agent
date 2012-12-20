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
import org.jmxexporter.util.DiscardingBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class Query {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ObjectName objectName;

    private String resultAlias;
    /**
     * JMX attributes to collect. As an array for {@link javax.management.MBeanServer#getAttributes(javax.management.ObjectName, String[])}
     */
    private Map<String, QueryAttribute> attributesByName = new HashMap<String, QueryAttribute>();
    private String[] attributeNames = new String[0];

    private List<OutputWriter> outputWriters;

    private final Queue<QueryResult> queryResults = new DiscardingBlockingQueue<QueryResult>(20);

    // TODO private Set<String> typeNames;

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


    public void performQuery(MBeanServer mbeanServer) {
        Set<ObjectName> matchingObjectNames = mbeanServer.queryNames(getObjectName(), null);
        for (ObjectName matchingObjectName : matchingObjectNames) {
            long epoch = System.currentTimeMillis();
            try {
                AttributeList jmxAttributes = mbeanServer.getAttributes(matchingObjectName, getAttributeNames());
                for (Attribute jmxAttribute : jmxAttributes.asList()) {
                    QueryAttribute queryAttribute = getAttribute(jmxAttribute.getName());
                    Collection<QueryResult> attributeResults = queryAttribute.parseAttribute(jmxAttribute, epoch);
                    queryResults.addAll(attributeResults);
                }
            } catch (Exception e) {
                logger.warn("Exception processing query {}", this, e);
            }
        }
    }

    public void writeResults() {
        for (OutputWriter writer : outputWriters) {
            // writer.
        }
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public void addResult(QueryResult queryResult) {
        queryResults.add(queryResult);
    }

    public Collection<QueryAttribute> getQueryAttributes() {
        return attributesByName.values();
    }

    public Query addAttribute(QueryAttribute attribute) {
        attributesByName.put(attribute.getName(), attribute);
        attributeNames = attributesByName.keySet().toArray(new String[0]);

        return this;
    }

    public Query addAttribute(String attributeName) {
        return addAttribute(new QueryAttribute(this, attributeName));
    }

    public String[] getAttributeNames() {
        return attributeNames;
    }

    public QueryAttribute getAttribute(String name) {
        return attributesByName.get(name);
    }

    public Queue<QueryResult> getResults() {
        return queryResults;
    }

    public String escapeObjectName(ObjectName on) {
        return on.getCanonicalKeyPropertyListString();
    }

    public String getResultName() {
        return resultAlias == null ? escapeObjectName(objectName) : resultAlias;
    }
}
