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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.jmxtrans.agent.graphite.GraphiteOutputWriterCommonSettings;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Kristoffer Erlandsson
 */
public class GraphitePlainTextTcpOutputWriterTest {

	@Rule
	public TcpLineServer tcpLineServer = new TcpLineServer();

	@Test
	public void reconnectsAfterServerClosesConnection() throws Exception {
		GraphitePlainTextTcpOutputWriter graphiteWriter = new GraphitePlainTextTcpOutputWriter();
		Map<String, String> config = new HashMap<>();
		config.put(GraphiteOutputWriterCommonSettings.SETTING_HOST, "127.0.0.1");
		config.put(GraphiteOutputWriterCommonSettings.SETTING_PORT, "" + tcpLineServer.getPort());
		graphiteWriter.postConstruct(config);
		// Write one metric to see it is received
		writeTestMetric(graphiteWriter);
		assertEventuallyReceived(tcpLineServer, hasSize(1));
		// Disconnect the Graphite writer
		tcpLineServer.disconnectAllClients();
		waitForErrorToBeDetectedByGraphiteWriter(graphiteWriter);
		writeTestMetric(graphiteWriter);
		// Write one metric and verify that it is received
		writeTestMetric(graphiteWriter);
		assertEventuallyReceived(tcpLineServer, hasSize(greaterThan(1)));
	}

	private void waitForErrorToBeDetectedByGraphiteWriter(GraphitePlainTextTcpOutputWriter writer) {
		for (int i = 0; i < 10; i++) {
			try {
				writer.writeQueryResult("foo", null, 1);
				writer.postCollect();
				Thread.sleep(20);
			} catch (Exception e) {
				return;
			}
		}
		fail("No error ocurred after closing server!");
	}

	private void writeTestMetric(GraphitePlainTextTcpOutputWriter writer) {
		try {
			writer.writeQueryResult("foo", null, 1);
			writer.postCollect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void assertEventuallyReceived(TcpLineServer server, Matcher<Collection<? extends Object>> matcher)
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
