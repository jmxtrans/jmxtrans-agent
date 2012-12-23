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
package org.jmxexporter.config;


import org.jmxexporter.JmxExporter;
import org.jmxexporter.Query;
import org.jmxexporter.QueryAttribute;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.InputStream;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ConfigurationTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    static Map<String, Query> queriesByResultName;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ConfigurationParser configurationParser = new ConfigurationParser();
        InputStream jsonFile = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/jmxexporter/sample1.json");
        JmxExporter configuration = configurationParser.parseConfiguration(jsonFile);
        queriesByResultName = new HashMap<String, Query>();
        for (Query query : configuration.getQueries()) {
            queriesByResultName.put(query.getResultName(), query);
        }
    }

    @Test
    public void validateBasicQuery() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get(objectName.getCanonicalKeyPropertyListString());
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultName(), is("name=PS Eden Space,type=MemoryPool"));
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThreshold"));
    }

    @Test
    public void validateAliasedQuery() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-aliased-query");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultName(), is("test-aliased-query"));
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
        assertThat(queryAttribute.getResultName(), is("CollectionUsageThresholdCount"));
    }

    @Test
    public void validateQueryWithAttributeAlias() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-attribute-with-alias");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultName(), is("test-attribute-with-alias"));
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
        assertThat(queryAttribute.getResultAlias(), is("test-alias"));
        assertThat(queryAttribute.getResultName(), is("test-alias"));
    }

    @Test
    public void validateQueryWithAttributes() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-attributes");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultName(), is("test-attributes"));

        assertThat(query.getQueryAttributes().size(), is(2));

        List<QueryAttribute> sortedAttributes = new ArrayList<QueryAttribute>();
        sortedAttributes.addAll(query.getQueryAttributes());
        Collections.sort(sortedAttributes);
        Iterator<QueryAttribute> sortedAttributesIterator = sortedAttributes.iterator();
        {
            QueryAttribute queryAttribute = sortedAttributesIterator.next();
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdExceeded"));
            assertThat(queryAttribute.getResultName(), is("CollectionUsageThresholdExceeded"));
        }
        {
            QueryAttribute queryAttribute = sortedAttributesIterator.next();
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdSupported"));
            assertThat(queryAttribute.getResultName(), is("CollectionUsageThresholdSupported"));
        }

    }

    @Test
    public void validateQueryWithAliasedAttributes() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-attributes-with-alias");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultName(), is("test-attributes-with-alias"));

        assertThat(query.getQueryAttributes().size(), is(3));

        List<QueryAttribute> sortedAttributes = new ArrayList<QueryAttribute>();
        sortedAttributes.addAll(query.getQueryAttributes());
        Collections.sort(sortedAttributes);
        Iterator<QueryAttribute> sortedAttributesIterator = sortedAttributes.iterator();
        {
            QueryAttribute queryAttribute = sortedAttributesIterator.next();
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
            assertThat(queryAttribute.getResultName(), is("CollectionUsageThresholdCount"));
        }
        {
            QueryAttribute queryAttribute = sortedAttributesIterator.next();
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdExceeded"));
            assertThat(queryAttribute.getResultName(), is("collection-usage-threshold-exceeded"));
        }
        {
            QueryAttribute queryAttribute = sortedAttributesIterator.next();
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdSupported"));
            assertThat(queryAttribute.getResultName(), is("collection-usage-threshold-supported"));
        }

    }
}
