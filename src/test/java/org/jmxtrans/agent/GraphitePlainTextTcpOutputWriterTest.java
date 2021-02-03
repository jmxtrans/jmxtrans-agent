/*
 * Copyright (c) 2010-2016 the original author or authors
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.jmxtrans.agent.graphite.GraphiteOutputWriterCommonSettings;
import org.jmxtrans.agent.testutils.FixedTimeClock;
import org.jmxtrans.agent.util.time.Clock;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Kristoffer Erlandsson
 */
public class GraphitePlainTextTcpOutputWriterTest {

	@Rule
	public TcpLineServer tcpLineServer = new TcpLineServer();

	private final Clock clock = new FixedTimeClock(33000);

	@Test
	public void reconnectsAfterServerClosesConnection() throws Exception {
		GraphitePlainTextTcpOutputWriter graphiteWriter = new GraphitePlainTextTcpOutputWriter();
		Map<String, String> config = new HashMap<>();
		config.put(GraphiteOutputWriterCommonSettings.SETTING_HOST, "127.0.0.1");
		config.put(GraphiteOutputWriterCommonSettings.SETTING_PORT, "" + tcpLineServer.getPort());
		config.put(GraphiteOutputWriterCommonSettings.SETTING_NAME_PREFIX, "bar.");
		graphiteWriter.postConstruct(config);
		graphiteWriter.setClock(clock);
		// Write one metric to see it is received
		writeTestMetric(graphiteWriter);
		assertEventuallyReceived(tcpLineServer, containsInAnyOrder("bar.foo 1 33"));
		// Disconnect the Graphite writer
		tcpLineServer.disconnectAllClients();
		waitForErrorToBeDetectedByGraphiteWriter(graphiteWriter);
		// Write one metric and verify that it is received
		writeTestMetric(graphiteWriter);
		writeTestMetric(graphiteWriter);
		assertEventuallyReceived(tcpLineServer, containsInAnyOrder("bar.foo 1 33", "bar.foo 2 33", "bar.foo 3 33"));
	}

	@Test
	public void filterNonNumericValues() throws Exception {
		GraphitePlainTextTcpOutputWriter writer = new GraphitePlainTextTcpOutputWriter();
		Map<String, String> config = new HashMap<>();
		config.put(GraphiteOutputWriterCommonSettings.SETTING_HOST, "127.0.0.1");
		config.put(GraphiteOutputWriterCommonSettings.SETTING_PORT, "" + tcpLineServer.getPort());
		config.put(GraphiteOutputWriterCommonSettings.SETTING_NAME_PREFIX, "bar.");
		config.put(GraphiteOutputWriterCommonSettings.SETTING_FILTER_NON_FLOAT, "true");
		writer.postConstruct(config);
		writer.setClock(clock);

		writer.writeQueryResult("metric", "type", 1);
		writer.writeQueryResult("metric", "type", null);
		writer.writeQueryResult("metric.2", "type", "non string");
		writer.writeQueryResult("metric.2", "type", "2");
		writer.writeQueryResult("metric.3", "type", "");
		writer.writeQueryResult("metric.3", "type", true);
		writer.postCollect();
		assertEventuallyReceived(tcpLineServer,
				containsInAnyOrder("bar.metric 1 33", "bar.metric.2 2 33", "bar.metric.3 1 33"));
		tcpLineServer.disconnectAllClients();
	}

	private void waitForErrorToBeDetectedByGraphiteWriter(GraphitePlainTextTcpOutputWriter writer) {
		for (int i = 0; i < 10; i++) {
			try {
				writer.writeQueryResult("foo", null, 4711 + i);
				writer.postCollect();
				Thread.sleep(20);
			} catch (Exception e) {
				return;
			}
		}
		fail("No error ocurred after closing server!");
	}

	private int counter;

	private void writeTestMetric(GraphitePlainTextTcpOutputWriter writer) {
		try {
			writer.writeQueryResult("foo", null, ++counter);
			writer.postCollect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void assertEventuallyReceived(TcpLineServer server, Matcher<Iterable<? extends String>> matcher)
			throws Exception {
		for (int i = 0; i < 100; i++) {
			if (matcher.matches(server.getReceivedLines())) {
				return;
			}
			Thread.sleep(10);
		}
		assertThat(server.getReceivedLines(), matcher);
	}
}
