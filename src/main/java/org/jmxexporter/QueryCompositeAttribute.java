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
import javax.management.openmbean.CompositeData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @see javax.management.openmbean.CompositeData
 */
public class QueryCompositeAttribute extends QueryAttribute {
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
    public QueryCompositeAttribute(Query query, String name, String alias, String... keys) {
        super(query, name, alias);
        this.keys = keys;
    }

    public String[] getKey() {
        return keys;
    }


    public Collection<QueryResult> parseAttribute(Attribute attribute, long epoch) {
        Object value = attribute.getValue();
        if (value instanceof CompositeData) {
            CompositeData compositeData = (CompositeData) value;
            Object[] objects = compositeData.getAll(keys);
            List<QueryResult> queryResults = new ArrayList<QueryResult>(keys.length);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                QueryResult queryResult = new QueryResult(
                        getQuery().getResultName(),
                        getResultName() + "." + key,
                        compositeData.get(key),
                        epoch);
                queryResults.add(queryResult);
            }
            return queryResults;
        } else {
            logger.warn("Ignore non CompositeData attribute value {}", attribute);
            return Collections.emptyList();
        }
    }
}
