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

import fr.xebia.management.jmxexporter.output.OutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class Query {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static class QueryAttribute {

        private final String name;

        private final String resultAlias;

        public QueryAttribute(String name) {
            this(name, null);
        }

        public QueryAttribute(String name, String resultAlias) {
            this.name = name;
            this.resultAlias = resultAlias;
        }

        public String getName() {
            return name;
        }

        public String getResultAlias() {
            return resultAlias;
        }

        public String getResultName() {
            return resultAlias == null ? name : resultAlias;
        }

        public Iterable<Result> parseAttribute(Attribute attribute, long epoch) {
            return Collections.singleton(new Result(resultAlias == null ? name : resultAlias, attribute.getValue(), epoch));
        }
    }

    /**
     * @see javax.management.openmbean.CompositeData
     */
    public static class QueryCompositeAttribute extends QueryAttribute {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        /**
         * @see javax.management.openmbean.CompositeType#keySet()
         */
        private final String[] keys;

        /**
         * @param name  name of the JMX attribute
         * @param alias
         * @param keys
         */
        public QueryCompositeAttribute(String name, String alias, String... keys) {
            super(name, alias);
            this.keys = keys;
        }

        public String[] getKey() {
            return keys;
        }


        public Iterable<Result> parseAttribute(Attribute attribute, long epoch) {
            Object value = attribute.getValue();
            if (value instanceof CompositeData) {
                CompositeData compositeData = (CompositeData) value;
                Object[] objects = compositeData.getAll(keys);
                List<Result> results = new ArrayList<Result>(keys.length);
                for (int i = 0; i < keys.length; i++) {
                    results.add(new Result(attribute.getName(), value, epoch));
                }
                return results;
            } else {
                logger.warn("Ignore non CompositeData attribute value {}", attribute);
                return Collections.emptyList();
            }
        }
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    private ObjectName objectName;

    public String[] getAttributeNames() {
        String[] attributeNames = new String[queryAttributes.size()];
        for (int i = 0; i < attributeNames.length; i++) {
            attributeNames[i] = queryAttributes.get(i).name;
        }
        return attributeNames;
    }


    private String resultAlias;
    /**
     * JMX attributes to collect. As an array for {@link javax.management.MBeanServer#getAttributes(javax.management.ObjectName, String[])}
     */
    private List<QueryAttribute> queryAttributes;
    private List<OutputWriter> outputWriters;
    private List<Result> results;
    private Set<String> typeNames;

    public void addResult(Result result) {
        results.add(result);
    }
}
