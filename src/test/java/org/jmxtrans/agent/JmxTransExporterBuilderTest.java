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

import org.junit.Test;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransExporterBuilderTest {

    @Test
    public void testParseConfiguration() throws Exception {
        JmxTransExporterBuilder builder = new JmxTransExporterBuilder();
        JmxTransExporter jmxTransExporter = builder.build("classpath:jmxtrans-agent.xml");

        assertThat(jmxTransExporter.collectInterval, is(11));
        assertThat(jmxTransExporter.collectIntervalTimeUnit, is(TimeUnit.SECONDS));

        OutputWriter decoratedOutputWriter = jmxTransExporter.outputWriter;
        // CircuitBreaker
        assertTrue(decoratedOutputWriter.getClass().equals(OutputWriterCircuitBreakerDecorator.class));
        OutputWriterCircuitBreakerDecorator circuitBreakerDecorator = (OutputWriterCircuitBreakerDecorator) decoratedOutputWriter;
        assertThat(circuitBreakerDecorator.isDisabled(), is(false));

        // Graphite Writer
        assertTrue(circuitBreakerDecorator.delegate.getClass().equals(GraphitePlainTextTcpOutputWriter.class));
        GraphitePlainTextTcpOutputWriter graphiteWriter = (GraphitePlainTextTcpOutputWriter) circuitBreakerDecorator.delegate;
        assertThat(graphiteWriter.graphiteServerSocketAddress.getPort(), is(2203));
        assertThat(graphiteWriter.graphiteServerSocketAddress.getHostName(), is("localhost"));
        assertThat(graphiteWriter.metricPathPrefix, is("app_123456.server.i876543."));

        assertThat(jmxTransExporter.queries.size(), is(13));

        Map<String, Query> queriesByResultAlias = indexQueriesByResultAlias(jmxTransExporter.queries);

        {
            Query query = queriesByResultAlias.get("os.systemLoadAverage");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=OperatingSystem")));
            assertThat(query.attribute, is("SystemLoadAverage"));
            assertThat(query.resultAlias, is("os.systemLoadAverage"));
            assertThat(query.key, is((String) null));
        }
        {
            Query query = queriesByResultAlias.get("jvm.heapMemoryUsage.used");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=Memory")));
            assertThat(query.attribute, is("HeapMemoryUsage"));
            assertThat(query.resultAlias, is("jvm.heapMemoryUsage.used"));
            assertThat(query.key, is("used"));
        }
    }

    @Test
    public void testParseConfiguration2() throws Exception {
        JmxTransExporterBuilder builder = new JmxTransExporterBuilder();
        JmxTransExporter jmxTransExporter = builder.build("classpath:jmxtrans-agent-2.xml");

        assertThat(jmxTransExporter.collectInterval, is(12));
        assertThat(jmxTransExporter.collectIntervalTimeUnit, is(TimeUnit.SECONDS));
        assertTrue(jmxTransExporter.outputWriter.getClass().equals(OutputWritersChain.class));

        OutputWritersChain outputWritersChain = (OutputWritersChain) jmxTransExporter.outputWriter;

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
            assertThat(writer.graphiteServerSocketAddress.getPort(), is(2003));
            assertThat(writer.graphiteServerSocketAddress.getHostName(), is("localhost"));
            assertThat(writer.metricPathPrefix, is("servers.localhost."));
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

        assertThat(jmxTransExporter.queries.size(), is(13));

        Map<String, Query> queriesByResultAlias = indexQueriesByResultAlias(jmxTransExporter.queries);

        {
            Query query = queriesByResultAlias.get("os.systemLoadAverage");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=OperatingSystem")));
            assertThat(query.attribute, is("SystemLoadAverage"));
            assertThat(query.resultAlias, is("os.systemLoadAverage"));
            assertThat(query.key, is((String) null));
        }
        {
            Query query = queriesByResultAlias.get("jvm.heapMemoryUsage.used");
            assertThat(query.objectName, is(new ObjectName("java.lang:type=Memory")));
            assertThat(query.attribute, is("HeapMemoryUsage"));
            assertThat(query.resultAlias, is("jvm.heapMemoryUsage.used"));
            assertThat(query.key, is("used"));
        }
    }

    Map<String, Query> indexQueriesByResultAlias(Iterable<Query> queries) {
        Map<String, Query> result = new HashMap<String, Query>();
        for (Query query : queries) {
            result.put(query.resultAlias, query);
        }
        return result;
    }
}
