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
package org.jmxtrans.embedded.config;


import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.Query;
import org.jmxtrans.embedded.QueryAttribute;
import org.jmxtrans.embedded.TestUtils;
import org.jmxtrans.embedded.output.*;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ConfigurationTest {

    static Map<String, Query> queriesByResultName;

    static EmbeddedJmxTrans embeddedJmxTrans;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ConfigurationParser configurationParser = new ConfigurationParser();
        embeddedJmxTrans = configurationParser.newEmbeddedJmxTrans("classpath:org/jmxtrans/embedded/jmxtrans-config-test.json");
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
    public void validateDisabledOutputWriters() throws MalformedObjectNameException {
        Query query = queriesByResultName.get("test-aliased-query");
        assertThat(query.getOutputWriters().size(), is(0));
        assertThat(query.getEffectiveOutputWriters().size(), is(4));

    }

    @Test
    public void validateDisabledRootOutputWriters() throws MalformedObjectNameException {
        Map<Class<? extends OutputWriter>, OutputWriter> outputWritersByClass = TestUtils.indexOutputWritersByClass(embeddedJmxTrans.getOutputWriters());

        assertThat(embeddedJmxTrans.getOutputWriters().size(), is(6));

        assertThat(outputWritersByClass.get(ConsoleWriter.class).isEnabled(), is(true));
        assertThat(outputWritersByClass.get(TestWriter1.class).isEnabled(), is(false));
        assertThat(outputWritersByClass.get(TestWriter2.class).isEnabled(), is(true));
        assertThat(outputWritersByClass.get(TestWriter3.class).isEnabled(), is(false));
        assertThat(outputWritersByClass.get(TestWriter4.class).isEnabled(), is(true));
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
        assertThat(query.getEffectiveOutputWriters().size(), is(5));
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
