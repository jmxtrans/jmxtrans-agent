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
package org.jmxtrans.agent;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.jmxtrans.agent.properties.PropertiesLoader;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransConfigurationXmlLoaderTest {

    @Test
    public void testParseConfiguration() throws Exception {
        JmxTransExporterConfiguration config = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-agent.xml").loadConfiguration();

        assertThat(config.collectInterval, is(11));
        assertThat(config.collectIntervalTimeUnit, is(TimeUnit.SECONDS));
        assertThat(config.getConfigReloadInterval(), equalTo(-1));

        OutputWriter decoratedOutputWriter = config.outputWriter;
        // CircuitBreaker
        assertTrue(decoratedOutputWriter.getClass().equals(OutputWriterCircuitBreakerDecorator.class));
        OutputWriterCircuitBreakerDecorator circuitBreakerDecorator = (OutputWriterCircuitBreakerDecorator) decoratedOutputWriter;
        assertThat(circuitBreakerDecorator.isDisabled(), is(false));

        // Graphite Writer
        assertTrue(circuitBreakerDecorator.delegate.getClass().equals(GraphitePlainTextTcpOutputWriter.class));
        GraphitePlainTextTcpOutputWriter graphiteWriter = (GraphitePlainTextTcpOutputWriter) circuitBreakerDecorator.delegate;
        assertThat(graphiteWriter.graphiteServerHostAndPort.getPort(), is(2203));
        assertThat(graphiteWriter.graphiteServerHostAndPort.getHost(), is("localhost"));
        assertThat(graphiteWriter.getMetricPathPrefix(), is("app_123456.server.i876543."));

        assertThat(config.queries.size(), is(13));

        Map<String, Query> queriesByResultAlias = indexQueriesByResultAlias(config.queries);

        {
            Query query = queriesByResultAlias.get("os.systemLoadAverage");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=OperatingSystem")));
            assertThat(query.getAttributes(), contains("SystemLoadAverage"));
            assertThat(query.resultAlias, is("os.systemLoadAverage"));
            assertThat(query.key, is((String) null));
        }
        {
            Query query = queriesByResultAlias.get("jvm.heapMemoryUsage.used");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=Memory")));
            assertThat(query.getAttributes(), contains("HeapMemoryUsage"));
            assertThat(query.resultAlias, is("jvm.heapMemoryUsage.used"));
            assertThat(query.key, is("used"));
        }
        Map<String, Invocation> invocationsByResultAlias = indexInvocationsByResultAlias(config.invocations);
        {
            Invocation invocation = invocationsByResultAlias.get("jvm.gc");
            assertThat(invocation.objectName, is(new ObjectName("java.lang:type=Memory")));
            assertThat(invocation.operationName, is("gc"));
            assertThat(invocation.resultAlias, is("jvm.gc"));
        }
    }

    @Test
    public void testParseConfiguration2() throws Exception {
        JmxTransExporterConfiguration config = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-agent-2.xml").loadConfiguration();

        assertThat(config.collectInterval, is(12));
        assertThat(config.collectIntervalTimeUnit, is(TimeUnit.SECONDS));
        assertTrue(config.outputWriter.getClass().equals(OutputWritersChain.class));

        OutputWritersChain outputWritersChain = (OutputWritersChain) config.outputWriter;

        assertThat(outputWritersChain.outputWriters.size(), is(2));

        {
            OutputWriter decoratedOutputWriter = outputWritersChain.outputWriters.get(0);
            // CircuitBreaker
            assertTrue(decoratedOutputWriter.getClass().equals(OutputWriterCircuitBreakerDecorator.class));
            OutputWriterCircuitBreakerDecorator circuitBreakerDecorator = (OutputWriterCircuitBreakerDecorator) decoratedOutputWriter;
            assertThat(circuitBreakerDecorator.isDisabled(), is(true));

            // Graphite Writer
            assertTrue(circuitBreakerDecorator.delegate.getClass().equals(GraphitePlainTextTcpOutputWriter.class));
            GraphitePlainTextTcpOutputWriter writer = (GraphitePlainTextTcpOutputWriter) circuitBreakerDecorator.delegate;
            assertThat(writer.graphiteServerHostAndPort.getPort(), is(2003));
            assertThat(writer.graphiteServerHostAndPort.getHost(), is("localhost"));
            assertThat(writer.getMetricPathPrefix(), is("servers.localhost."));
        }
        {
            OutputWriter decoratedOutputWriter = outputWritersChain.outputWriters.get(1);
            // Circuit Breaker
            assertTrue(decoratedOutputWriter.getClass().equals(OutputWriterCircuitBreakerDecorator.class));
            OutputWriterCircuitBreakerDecorator circuitBreakerDecorator = (OutputWriterCircuitBreakerDecorator) decoratedOutputWriter;
            assertThat(circuitBreakerDecorator.isDisabled(), is(true));

            // Console Writer
            assertTrue(circuitBreakerDecorator.delegate.getClass().equals(ConsoleOutputWriter.class));

        }

        assertThat(config.queries.size(), is(13));

        Map<String, Query> queriesByResultAlias = indexQueriesByResultAlias(config.queries);

        {
            Query query = queriesByResultAlias.get("os.systemLoadAverage");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=OperatingSystem")));
            assertThat(query.getAttributes(), contains("SystemLoadAverage"));
            assertThat(query.resultAlias, is("os.systemLoadAverage"));
            assertThat(query.key, is((String) null));
        }
        {
            Query query = queriesByResultAlias.get("jvm.heapMemoryUsage.used");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=Memory")));
            assertThat(query.getAttributes(), contains("HeapMemoryUsage"));
            assertThat(query.resultAlias, is("jvm.heapMemoryUsage.used"));
            assertThat(query.key, is("used"));
        }
    }

    @Test
    public void testParseConfigurationMultipleAttributes() throws Exception {
        JmxTransExporterConfiguration config = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-multiple-attributes-test.xml").loadConfiguration();
        assertThat(config.queries, hasSize(1));
        Query query = config.queries.get(0);
        assertThat(query.getAttributes(), contains("ThreadCount", "TotalStartedThreadCount"));
    }

    @Test
    public void testNoAttributesSpecifiedGeneratesWildcardQuery() throws Exception {
        JmxTransExporterConfiguration config = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-no-attributes-specified-generates-wildcard-query-test.xml").loadConfiguration();
        assertThat(config.queries, hasSize(1));
        Query query = config.queries.get(0);
        assertThat(query.getAttributes(), emptyIterable());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseConfigurationAttributeAndAttributesMutuallyExclusive() throws Exception {
        new JmxTransConfigurationXmlLoader("classpath:jmxtrans-attribute-attributes-exclusive-test.xml").loadConfiguration();
    }
    
    @Test
    public void testParseConfigReload() throws Exception {
        JmxTransExporterConfiguration config = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-config-reload-test.xml").loadConfiguration();
        assertThat(config.getConfigReloadInterval(), equalTo(2));
        
    }
    
    @Test
    public void testExternalPropertiesSourceIsUsed() throws Exception {
        PropertiesLoader propertiesLoader = new PropertiesLoader() {
            
            @Override
            public Map<String, String> loadProperties() {
                HashMap<String, String> m = new HashMap<>();
                m.put("jmxtrans.agent.collect.interval", "999");
                return m;
            }
        };
        JmxTransConfigurationXmlLoader configLoader = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-external-properties-test.xml", propertiesLoader);
        JmxTransExporterConfiguration config = configLoader.build(configLoader);
        assertThat(config.getCollectInterval(), equalTo(999));
    }

    @Test
    public void testErrorOnLoadingExternalPropertiesDoesNotPropagate() throws Exception {
        PropertiesLoader propertiesLoader = new PropertiesLoader() {
            
            @Override
            public Map<String, String> loadProperties() {
                throw new RuntimeException("Expected - thrown by test");
            }
        };
        JmxTransConfigurationXmlLoader configLoader = new JmxTransConfigurationXmlLoader("classpath:jmxtrans-external-properties-test.xml", propertiesLoader);
        JmxTransExporterConfiguration config = configLoader.build(configLoader);
        assertThat(config.getCollectInterval(), equalTo(222));
    }

    Map<String, Query> indexQueriesByResultAlias(Iterable<Query> queries) {
        Map<String, Query> result = new HashMap<>();
        for (Query query : queries) {
            result.put(query.resultAlias, query);
        }
        return result;
    }

    Map<String, Invocation> indexInvocationsByResultAlias(Iterable<Invocation> invocations) {
        Map<String, Invocation> result = new HashMap<String, Invocation>();
        for (Invocation invocation : invocations) {
            result.put(invocation.resultAlias, invocation);
        }
        return result;
    }
}
