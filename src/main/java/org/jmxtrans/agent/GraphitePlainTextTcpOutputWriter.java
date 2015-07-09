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

import org.jmxtrans.agent.util.net.HostAndPort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.jmxtrans.agent.util.ConfigurationUtils.getInt;
import static org.jmxtrans.agent.util.ConfigurationUtils.getString;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class GraphitePlainTextTcpOutputWriter extends AbstractOutputWriter implements OutputWriter {

    public final static String SETTING_HOST = "host";
    public final static String SETTING_PORT = "port";
    public static final int SETTING_PORT_DEFAULT_VALUE = 2003;
    public final static String SETTING_NAME_PREFIX = "namePrefix";
    public final static String SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS = "socket.connectTimeoutInMillis";
    public final static int SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE = 500;

    private final static Charset UTF_8 = Charset.forName("UTF-8");
    protected String metricPathPrefix;
    protected HostAndPort graphiteServerHostAndPort;
    private Socket socket;
    private Writer writer;
    private int socketConnectTimeoutInMillis = SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE;

    @Override
    public void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);

        graphiteServerHostAndPort = new HostAndPort(
                getString(settings, SETTING_HOST),
                getInt(settings, SETTING_PORT, SETTING_PORT_DEFAULT_VALUE));
        metricPathPrefix = getString(settings, SETTING_NAME_PREFIX, null);
        socketConnectTimeoutInMillis = getInt(settings,
                SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS,
                SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE);

        logger.log(getInfoLevel(), "GraphitePlainTextTcpOutputWriter is configured with " + graphiteServerHostAndPort + ", metricPathPrefix=" + metricPathPrefix +
                ", socketConnectTimeoutInMillis=" + socketConnectTimeoutInMillis);
    }

    /**
     * {@link java.net.InetAddress#getLocalHost()} may not be known at JVM startup when the process is launched as a Linux service.
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

    @Override
    public void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public void writeQueryResult(@Nonnull String metricName, @Nullable String type, @Nullable Object value) throws IOException {
        String msg = buildMetricPathPrefix() + metricName + " " + value + " " + TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        try {
            ensureGraphiteConnection();
            if (logger.isLoggable(getTraceLevel())) {
                logger.log(getTraceLevel(), "Send '" + msg + "' to " + graphiteServerHostAndPort);
            }
            writer.write(msg + "\n");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception sending '" + msg + "' to " + graphiteServerHostAndPort, e);
            releaseGraphiteConnection();
            throw e;
        }
    }

    private void releaseGraphiteConnection() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Exception closing writer for socket " + socket, e);
            }
            writer = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Exception closing socket " + socket, e);
            }
            socket = null;
        }
    }

    private void ensureGraphiteConnection() throws IOException {
        boolean socketIsValid;
        try {
            socketIsValid = socket != null &&
                    socket.isConnected()
                    && socket.isBound()
                    && !socket.isClosed()
                    && !socket.isInputShutdown()
                    && !socket.isOutputShutdown();
        } catch (Exception e) {
            socketIsValid = false;
        }
        if (!socketIsValid) {
            writer = null;
            try {
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.connect(
                        new InetSocketAddress(graphiteServerHostAndPort.getHost(), graphiteServerHostAndPort.getPort()),
                        socketConnectTimeoutInMillis);
            } catch (IOException e) {
                ConnectException ce = new ConnectException("Exception connecting to " + graphiteServerHostAndPort);
                ce.initCause(e);
                throw ce;
            }
        }
        if (writer == null) {
            writer = new OutputStreamWriter(socket.getOutputStream(), UTF_8);
        }
    }

    @Override
    public void postCollect() throws IOException {
        if (writer == null) {
            return;
        }
        
        try {
            writer.flush();
        } catch (IOException e) {
			releaseGraphiteConnection();
            throw e;
        }
    }

    @Override
    public String toString() {
        return "GraphitePlainTextTcpOutputWriter{" +
                ", " + graphiteServerHostAndPort +
                ", metricPathPrefix='" + metricPathPrefix + '\'' +
                '}';
    }
}
