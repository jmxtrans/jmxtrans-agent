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
package org.jmxtrans.embedded.config;


import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.Query;
import org.jmxtrans.embedded.QueryAttribute;
import org.jmxtrans.embedded.TestUtils;
import org.jmxtrans.embedded.output.NoOpWriter;
import org.jmxtrans.embedded.output.OutputWriter;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ConfigurationMergeTest {

    static Map<String, Query> queriesByResultName;

    static EmbeddedJmxTrans embeddedJmxTrans;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ConfigurationParser configurationParser = new ConfigurationParser();
        embeddedJmxTrans = configurationParser.newEmbeddedJmxTrans(
                "classpath:org/jmxtrans/embedded/jmxtrans-config-merge-1-test.json",
                "classpath:org/jmxtrans/embedded/jmxtrans-config-merge-2-test.json"
        );
        queriesByResultName = TestUtils.indexQueriesByAliasOrName(embeddedJmxTrans.getQueries());
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
        assertThat(embeddedJmxTrans.getNumQueryThreads(), is(3));
        assertThat(embeddedJmxTrans.getQueryIntervalInSeconds(), is(5));
        assertThat(embeddedJmxTrans.getExportIntervalInSeconds(), is(10));
        assertThat(embeddedJmxTrans.getNumExportThreads(), is(2));
    }
}
