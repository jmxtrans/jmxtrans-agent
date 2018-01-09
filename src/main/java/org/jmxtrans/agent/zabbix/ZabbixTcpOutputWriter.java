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
package org.jmxtrans.agent.zabbix;

import static org.jmxtrans.agent.util.ConfigurationUtils.getInt;
import static org.jmxtrans.agent.util.ConfigurationUtils.getString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jmxtrans.agent.AbstractOutputWriter;
import org.jmxtrans.agent.OutputWriter;
import org.jmxtrans.agent.util.io.IoUtils;
import org.jmxtrans.agent.util.net.HostAndPort;

/**
 * @author Steve McDuff
 */
public class ZabbixTcpOutputWriter extends AbstractOutputWriter implements OutputWriter
{

    private static final int ZABBIX_HEADER_LENGTH = 13;
    public final static String SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS = "socket.connectTimeoutInMillis";
    public final static int SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE = 500;

    protected HostAndPort zabbixServerHostAndPort;
    private Socket socket;
    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private byte[] readBuffer = new byte[10000];
    private int socketConnectTimeoutInMillis = SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE;
    private ZabbixMetricMessageBuilder messageBuilder;

    private int printMetricCount = 0;
    private int metricBatchSize;
    
    private boolean failedConnection = false;

    @Override
    public void postConstruct(Map<String, String> settings)
    {
        super.postConstruct(settings);

        zabbixServerHostAndPort = new HostAndPort(getString(settings, ZabbixOutputWriterCommonSettings.SETTING_HOST),
            getInt(settings, ZabbixOutputWriterCommonSettings.SETTING_PORT,
                ZabbixOutputWriterCommonSettings.SETTING_PORT_DEFAULT_VALUE));
        messageBuilder = new ZabbixMetricMessageBuilder(
            ZabbixOutputWriterCommonSettings.getConfiguredHostName(settings));
        socketConnectTimeoutInMillis = getInt(settings, SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS,
            SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE);

        metricBatchSize = ZabbixOutputWriterCommonSettings.getMetricBatchSize(settings);

        logger.log(getInfoLevel(),
            "ZabbixTcpOutputWriter is configured with " + zabbixServerHostAndPort + ", serverName=" +
                messageBuilder.getHostName() + ", socketConnectTimeoutInMillis=" + socketConnectTimeoutInMillis +
                ", metricBatchSize=" + metricBatchSize);
    }

    @Override
    public void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException
    {
        writeQueryResult(invocationName, null, value);
    }

    private byte[] messageHeader = "{\"request\":\"sender data\",\"data\":[".getBytes(StandardCharsets.UTF_8);
    private byte[] comma = ",".getBytes(StandardCharsets.UTF_8);
    private byte[] messageFooter = "]}".getBytes(StandardCharsets.UTF_8);
    

    @Override
    public void writeQueryResult(@Nonnull String metricName, @Nullable String type, @Nullable Object value)
        throws IOException
    {
        String msg = messageBuilder.buildMessage(metricName, value,
            TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS));

        try
        {
            if (logger.isLoggable(getTraceLevel()))
            {
                logger.log(getTraceLevel(), "Send '" + msg + "' to " + zabbixServerHostAndPort);
            }

            if (printMetricCount == 0)
            {
                // print the header
                writeMessage(messageHeader);
            }
            else
            {
                writeMessage(comma);
            }

            printMetricCount += 1;
            writeMessage(msg);

            if (printMetricCount >= metricBatchSize)
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.fine(
                        "Reached batch size maximum of " + metricBatchSize + " .Forcing message output to Zabbix.");
                }
                postCollect();
            }

        }
        catch (IOException e)
        {
            logger.log(Level.WARNING,
                "Exception sending '" + msg + "' of size : " + out.size() + "bytes to " + zabbixServerHostAndPort, e);

            releaseZabbixConnection();
            throw e;
        }
    }

    private void writeMessage(String msg) throws IOException
    {
        if (logger.isLoggable(Level.FINEST))
        {
            logger.finest("Print : " + msg);
        }

        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        writeMessage(bytes);
    }

    protected void writeMessage(byte[] data) throws IOException
    {
        out.write(data);
    }

    private void writeZabbixHeader(int length) throws IOException
    {
        byte[] zabbixHeader = new byte[]
        { 'Z', 'B', 'X', 'D', '\1', (byte) (length & 0xFF), (byte) ((length >> 8) & 0x00FF),
            (byte) ((length >> 16) & 0x0000FF), (byte) ((length >> 24) & 0x000000FF), '\0', '\0', '\0', '\0' };

        socket.getOutputStream().write(zabbixHeader);
    }

    private void releaseZabbixConnection()
    {
        IoUtils.closeQuietly(out);
        IoUtils.closeQuietly(socket);
    }

    private void ensureZabbixConnection() throws IOException
    {
        boolean socketIsValid;
        try
        {
            socketIsValid = socket != null && socket.isConnected() && socket.isBound() && !socket.isClosed() &&
                !socket.isInputShutdown() && !socket.isOutputShutdown();
        }
        catch (Exception e)
        {
            socketIsValid = false;
        }
        if (!socketIsValid)
        {
            long start = System.currentTimeMillis();
            try
            {
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.connect(
                    new InetSocketAddress(zabbixServerHostAndPort.getHost(), zabbixServerHostAndPort.getPort()),
                    socketConnectTimeoutInMillis);
            }
            catch (IOException e)
            {
                ConnectException ce = new ConnectException("Exception connecting to " + zabbixServerHostAndPort);
                ce.initCause(e);
                throw ce;
            }
            long end = System.currentTimeMillis();

            if (logger.isLoggable(Level.FINE))
            {
                logger.fine("Connect time : " + (end - start));
            }
        }
    }

    @Override
    public void postCollect() throws IOException
    {
        if (printMetricCount == 0)
        {
            // nothing to print
            return;
        }

        try
        {
            writeMessage(messageFooter);

            byte[] byteArray = out.toByteArray();
            out.reset();

            if (logger.isLoggable(Level.FINEST))
            {
                String msg = new String(byteArray, StandardCharsets.UTF_8);
                logger.finest("message : " + msg);
            }

            ensureZabbixConnection();

            writeZabbixHeader(byteArray.length);
            socket.getOutputStream().write(byteArray);

            printMetricCount = 0;

            drainInputStream();

            // Zabbix seems to drop connections on every message.
            // Forcing a drop prevents timeout exceptions for subsequent messages.
            releaseZabbixConnection();
            failedConnection = false;
        }
        catch (IOException e)
        {
            // log subsequent failures at a lower log level.
            if( failedConnection ) {
                logger.log(Level.FINE, "Exception flushing the stream to " + zabbixServerHostAndPort, e);
            }
            else {
                logger.log(Level.WARNING, "Exception flushing the stream to " + zabbixServerHostAndPort, e);
            }
            releaseZabbixConnection();
            throw e;
        }
    }

    private void drainInputStream() throws IOException
    {
        try
        {
            int readSize = socket.getInputStream().read(readBuffer);
            if (logger.isLoggable(Level.FINE))
            {

                String message = extractZabbixResponseString(readSize, readBuffer);
                logger.log(Level.FINE, message);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to read from scoket " + zabbixServerHostAndPort, ex);
        }
    }

    private String extractZabbixResponseString(int readSize, byte[] buffer)
    {
        String msg = "";
        if (readSize > ZABBIX_HEADER_LENGTH)
        {
            // skip the zabbix header
            msg = new String(buffer, ZABBIX_HEADER_LENGTH, readSize - ZABBIX_HEADER_LENGTH, StandardCharsets.UTF_8);
        }
        return msg;
    }

    @Override
    public String toString()
    {
        return "ZabbixTcpOutputWriter{" + ", " + zabbixServerHostAndPort + ", serverName='" +
            messageBuilder.getHostName() + '\'' + '}';
    }

    @Override
    public void preDestroy()
    {
        super.preDestroy();
        releaseZabbixConnection();
    }

}
