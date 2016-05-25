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

import static org.jmxtrans.agent.graphite.GraphiteOutputWriterCommonSettings.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.jmxtrans.agent.graphite.GraphiteMetricMessageBuilder;
import org.jmxtrans.agent.util.net.HostAndPort;
import org.jmxtrans.agent.util.time.Clock;
import org.jmxtrans.agent.util.time.SystemCurrentTimeMillisClock;

/**
 * Output writer for writing to Graphite using UDP.
 * 
 * @author Kristoffer Erlandsson
 */
public class GraphiteUdpOutputWriter extends AbstractOutputWriter {

    final static Charset CHARSET_FOR_UDP_PACKET = Charset.forName("UTF-8");
    private HostAndPort graphiteServerHostAndPort;
    private UdpMessageSender messageSender;
    private Clock clock;
    private GraphiteMetricMessageBuilder messageBuilder;

    @Override
    public void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);
        graphiteServerHostAndPort = getHostAndPort(settings);
        messageBuilder = new GraphiteMetricMessageBuilder(getConfiguredMetricPrefixOrNull(settings));
        messageSender = new UdpMessageSender(graphiteServerHostAndPort);
        clock = new SystemCurrentTimeMillisClock();
        logger.log(getInfoLevel(), "GraphiteUdpOutputWriter is configured with " + graphiteServerHostAndPort
                + ", metricPathPrefix=" + messageBuilder.getPrefix());
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public void writeQueryResult(String metricName, String metricType, Object value) throws IOException {
        String msg = messageBuilder.buildMessage(metricName, value, TimeUnit.SECONDS.convert(clock.getCurrentTimeMillis(), TimeUnit.MILLISECONDS));
        logMessageIfTraceLoggable(msg);
        tryWriteMsg(msg + "\n");
    }

    private void logMessageIfTraceLoggable(String msg) {
        if (logger.isLoggable(getTraceLevel())) {
            logger.log(getTraceLevel(), "Send '" + msg + "' to " + graphiteServerHostAndPort);
        }
    }

    private void tryWriteMsg(String msg) throws IOException {
        try {
            messageSender.send(msg);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception sending '" + msg + "' to " + graphiteServerHostAndPort, e);
            throw e;
        }
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
        if (messageSender != null) {
            messageSender.close();
        }
    }

    @Override
    public String toString() {
        return "GraphiteUdpOutputWriter{" +
                ", " + graphiteServerHostAndPort +
                ", metricPathPrefix='" + messageBuilder.getPrefix() + "'" +
                "}";
    }

    /** Test hook */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    private static class UdpMessageSender {

        private final DatagramSocket clientSocket;
        private final HostAndPort hostAndPort;

        public UdpMessageSender(HostAndPort hostAndPort) {
            this.hostAndPort = hostAndPort;
            try {
                clientSocket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException("Failed to create DatagramSocket", e);
            }
        }

        public void send(String msg) throws IOException {
            DatagramPacket packetToSend = createUdpPacket(msg);
            clientSocket.send(packetToSend);
        }

        private DatagramPacket createUdpPacket(String msg) throws SocketException {
            byte[] messageBytes = msg.getBytes(CHARSET_FOR_UDP_PACKET);
            InetSocketAddress adress = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
            return new DatagramPacket(messageBytes, messageBytes.length, adress);
        }

        public void close() {
            clientSocket.close();
        }
    }

}
