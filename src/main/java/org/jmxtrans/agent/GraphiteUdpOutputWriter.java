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

import static org.jmxtrans.agent.util.ConfigurationUtils.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.jmxtrans.agent.util.net.HostAndPort;

/**
 * Output writer for writing to Graphite using UDP.
 * 
 * @author Kristoffer Erlandsson
 */
public class GraphiteUdpOutputWriter extends AbstractOutputWriter {

    final static Charset CHARSET_FOR_UDP_PACKET = Charset.forName("UTF-8");
    private final static String SETTING_HOST = "host";
    private final static String SETTING_PORT = "port";
    private static final int SETTING_PORT_DEFAULT_VALUE = 2003;
    private final static String SETTING_NAME_PREFIX = "namePrefix";
    private HostAndPort graphiteServerHostAndPort;
    private String metricPathPrefix;
    private UdpMessageSender messageSender;
    private Clock clock;

    @Override
    public void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);
        graphiteServerHostAndPort = new HostAndPort(
                getString(settings, SETTING_HOST),
                getInt(settings, SETTING_PORT, SETTING_PORT_DEFAULT_VALUE));
        metricPathPrefix = getString(settings, SETTING_NAME_PREFIX, null);
        messageSender = new UdpMessageSender(graphiteServerHostAndPort);
        clock = new SystemCurrentTimeMillisClock();
        logger.log(getInfoLevel(), "GraphiteUdpOutputWriter is configured with " + graphiteServerHostAndPort
                + ", metricPathPrefix=" + metricPathPrefix);
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public void writeQueryResult(String metricName, String metricType, Object value) throws IOException {
        String msg = buildMetricPathPrefix() + metricName + " " + value + " " + clock.getCurrentInTimeSeconds() + "\n";
        logMessageIfTraceLoggable(msg);
        tryWriteMsg(msg);
    }

    /**
     * {@link java.net.InetAddress#getLocalHost()} may not be known at JVM startup when the process is launched as a
     * Linux service.
     *
     * @return
     */
    protected String buildMetricPathPrefix() {
        if (metricPathPrefix != null) {
            return metricPathPrefix;
        }
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName().replaceAll("\\.", "_");
        } catch (UnknownHostException e) {
            hostname = "#unknown#";
        }
        metricPathPrefix = "servers." + hostname + ".";
        return metricPathPrefix;
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
                ", metricPathPrefix='" + metricPathPrefix + "'" +
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

    interface Clock {
        long getCurrentInTimeSeconds();
    }

    private static class SystemCurrentTimeMillisClock implements Clock {

        @Override
        public long getCurrentInTimeSeconds() {
            return TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
