/*
 * Copyright 1012-2013 the original author or authors.
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
import org.jmxexporter.TestUtils;
import org.jmxexporter.output.NoOpWriter;
import org.jmxexporter.output.OutputWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ConfigurationTest {

    static Map<String, Query> queriesByResultName;

    static JmxExporter jmxExporter;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ConfigurationParser configurationParser = new ConfigurationParser();
        jmxExporter = configurationParser.newJmxExporter("classpath:org/jmxexporter/jmxexporter-config-test.json");
        queriesByResultName = TestUtils.indexQueriesByAliasOrName(jmxExporter.getQueries());
    }

    @Test
    public void validateBasicQuery() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get(objectName.toString());
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultAlias(), nullValue());
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThreshold"));
    }

    @Test
    public void validateAliasedQuery() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-aliased-query");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultAlias(), is("test-aliased-query"));
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
    }

    @Test
    public void validateQueryWithAttributeAlias() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-attribute-with-alias");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultAlias(), is("test-attribute-with-alias"));
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
        assertThat(queryAttribute.getResultAlias(), is("test-alias"));
    }

    @Test
    public void validateQueryWithAttributes() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-attributes");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultAlias(), is("test-attributes"));

        assertThat(query.getQueryAttributes().size(), is(2));

        Map<String, QueryAttribute> queryAttributes = TestUtils.indexQueryAttributesByAliasOrName(query.getQueryAttributes());

        {
            QueryAttribute queryAttribute = queryAttributes.get("CollectionUsageThresholdExceeded");
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdExceeded"));
            assertThat(queryAttribute.getResultAlias(), nullValue());
        }
        {
            QueryAttribute queryAttribute = queryAttributes.get("CollectionUsageThresholdSupported");
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdSupported"));
            assertThat(queryAttribute.getResultAlias(), nullValue());
        }

    }

    @Test
    public void validateQueryWithAliasedAttributes() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-attributes-with-alias");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultAlias(), is("test-attributes-with-alias"));

        assertThat(query.getQueryAttributes().size(), is(3));

        Map<String, QueryAttribute> queryAttributes = TestUtils.indexQueryAttributesByAliasOrName(query.getQueryAttributes());


        {
            QueryAttribute queryAttribute = queryAttributes.get("collection-usage-threshold-supported");
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdSupported"));
            assertThat(queryAttribute.getResultAlias(), is("collection-usage-threshold-supported"));
        }
        {
            QueryAttribute queryAttribute = queryAttributes.get("CollectionUsageThresholdCount");
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
            assertThat(queryAttribute.getResultAlias(), nullValue());
        }
        {
            QueryAttribute queryAttribute = queryAttributes.get("collection-usage-threshold-exceeded");
            assertThat(queryAttribute.getName(), is("CollectionUsageThresholdExceeded"));
            assertThat(queryAttribute.getResultAlias(), is("collection-usage-threshold-exceeded"));
        }


    }

    @Test
    public void validateQueryWithOutputWriter() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space");
        Query query = queriesByResultName.get("test-with-outputwriter");
        assertThat(query.getObjectName(), is(objectName));
        assertThat(query.getResultAlias(), is("test-with-outputwriter"));
        assertThat(query.getQueryAttributes().size(), is(1));
        QueryAttribute queryAttribute = query.getQueryAttributes().iterator().next();
        assertThat(queryAttribute.getName(), is("CollectionUsageThresholdCount"));
        assertThat(query.getOutputWriters().size(), is(1));
        OutputWriter outputWriter = query.getOutputWriters().get(0);
        assertThat(outputWriter, instanceOf(NoOpWriter.class));
    }

    @Test
    public void validateTuningParameters() {
        assertThat(jmxExporter.getNumQueryThreads(), is(3));
        assertThat(jmxExporter.getQueryIntervalInSeconds(), is(5));
        assertThat(jmxExporter.getExportIntervalInSeconds(), is(10));
        assertThat(jmxExporter.getNumExportThreads(), is(2));
    }
}
