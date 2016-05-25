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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.jmxtrans.agent.testutils.FixedTimeClock;
import org.jmxtrans.agent.util.time.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Tests for GraphiteUdpOutputWriter.
 * 
 * @author Kristoffer Erlandsson
 */
public class GraphiteUdpOutputWriterTest {

    @Rule
    public UdpServer udpServer = new UdpServer();

    private Clock clock = new FixedTimeClock(33000);

    private GraphiteUdpOutputWriter writer;

    @Before
    public void createWriter() throws Exception {
        writer = new GraphiteUdpOutputWriter();
        writer.postConstruct(testSettings());
        writer.setClock(clock);
    }

    @Test
    public void oneQueryResult() throws Exception {
        writer.writeQueryResult("metric", "type", 1);
        assertEventuallyReceived(udpServer, contains("foo.metric 1 33\n"));
    }

    @Test
    public void manyQueryResults() throws Exception {
        writer.writeQueryResult("metric", "type", 1);
        writer.writeQueryResult("metric.2", "type", 2);
        writer.writeQueryResult("metric.3", "type", 3);
        assertEventuallyReceived(udpServer,
                containsInAnyOrder("foo.metric 1 33\n", "foo.metric.2 2 33\n", "foo.metric.3 3 33\n"));
    }

    @Test
    public void oneInvocationResult() throws Exception {
        writer.writeInvocationResult("invoke", 123);
        assertEventuallyReceived(udpServer, contains("foo.invoke 123 33\n"));
    }

    @After
    public void destroyWriter() {
        writer.preDestroy();
    }

    private Map<String, String> testSettings() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put("host", "127.0.0.1");
        settings.put("port", "" + udpServer.getPort());
        settings.put("namePrefix", "foo.");
        return settings;
    }

    /**
     * Waits for one second for the received messages of the UdpServer to match the matcher.
     */
    public void assertEventuallyReceived(UdpServer server, Matcher<Iterable<? extends String>> matcher)
            throws Exception {
        for (int i = 0; i < 100; i++) {
            server.receiveAvailableDatagrams();
            if (matcher.matches(server.getReceivedMessages())) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(server.getReceivedMessages(), matcher);
    }

    private static class UdpServer implements TestRule {

        private List<String> receivedMessages = new ArrayList<>();
        private DatagramChannel channel;

        public void openChannel() throws Exception {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            channel.configureBlocking(false);
        }

        private void closeChannelAndClearMessages() throws IOException {
            if (channel != null) {
                channel.close();
            }
            receivedMessages.clear();
        }

        public List<String> getReceivedMessages() {
            return receivedMessages;
        }

        public int getPort() throws IOException {
            return ((InetSocketAddress) channel.getLocalAddress()).getPort();
        }

        public void receiveAvailableDatagrams() throws IOException {
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                if (channel.receive(buffer) == null) {
                    break;
                }
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                String result = new String(bytes, GraphiteUdpOutputWriter.CHARSET_FOR_UDP_PACKET);
                receivedMessages.add(result);
            }
        }

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    try {
                        openChannel();
                        base.evaluate();
                    } finally {
                        closeChannelAndClearMessages();
                    }
                }

            };
        }

    }
}
