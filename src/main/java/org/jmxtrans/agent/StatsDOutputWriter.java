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

import org.jmxtrans.agent.util.CachingReference;
import org.jmxtrans.agent.util.ConfigurationUtils;
import org.jmxtrans.agent.util.StringUtils2;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StatsDOutputWriter extends AbstractOutputWriter implements OutputWriter {

    public final static String SETTING_HOST = "host";
    public final static String SETTING_PORT = "port";
    public final static String SETTING_ROOT_PREFIX = "metricName";
    public final static String SETTING_BUFFER_SIZE = "bufferSize";
    private final static int SETTING_DEFAULT_BUFFER_SIZE = 1024;

    private ByteBuffer sendBuffer;
    private String metricNamePrefix;
    /**
     * Using a {@link CachingReference} instead of a raw {@link InetSocketAddress} allows to handle a change
     */
    private CachingReference<InetSocketAddress> addressReference;
    private DatagramChannel channel;

    @Override
    public void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);

        final String host = ConfigurationUtils.getString(settings, SETTING_HOST);
        final Integer port = ConfigurationUtils.getInt(settings, SETTING_PORT);
        metricNamePrefix = ConfigurationUtils.getString(settings, SETTING_ROOT_PREFIX, getHostName().replaceAll("\\.", "_"));

        if (port == null || StringUtils2.isNullOrEmpty(host)) {
            throw new RuntimeException("Host and/or port cannot be null");
        }

        int bufferSize = ConfigurationUtils.getInt(settings, SETTING_BUFFER_SIZE, SETTING_DEFAULT_BUFFER_SIZE);
        sendBuffer = ByteBuffer.allocate(bufferSize);

        addressReference = new CachingReference<InetSocketAddress>(30, TimeUnit.SECONDS) {
            @Nonnull
            @Override
            protected InetSocketAddress newObject() {
                return new InetSocketAddress(host, port);
            }
        };
        try {
            channel = DatagramChannel.open();
        } catch (IOException e) {
            throw new RuntimeException("Exception opening datagram channel", e);
        }

        logger.info(String.format("StatsDOutputWriter[host=%s, port=%d, metricNamePrefix=%s]", host, port, metricNamePrefix));
    }

    protected String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e1) {
                return "unknown.host";
            }
        }
    }

    @Override
    public void postCollect() throws IOException {
        // Ensure data flush
        flush();
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public void writeQueryResult(String metricName, String metricType, Object value) throws IOException {
        String stats = metricNamePrefix + "." + metricName + ":" + value + "|c\n";
        if (logger.isLoggable(getDebugLevel())) {
            logger.log(getDebugLevel(), "Sending msg: " + stats);
        }
        doSend(stats);
    }

    private synchronized boolean doSend(String stat) {
        try {
            final byte[] data = stat.getBytes("utf-8");

            // If we're going to go past the threshold of the buffer then flush.
            // the +1 is for the potential '\n' in multi_metrics below
            if (sendBuffer.remaining() < (data.length + 1)) {
                flush();
            }

            if (sendBuffer.remaining() < (data.length + 1)) {
                logger.warning("Given data too big (" + data.length + "bytes) for the buffer size (" + sendBuffer.remaining() + "bytes), skip it: "
                        + StringUtils2.abbreviate(stat, 20));
            }

            sendBuffer.put(data); // append the data
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format(
                    "Could not send stat %s to host %s:%d", sendBuffer.toString(), addressReference.get().getHostName(),
                    addressReference.get().getPort()), e);
            return false;
        }
    }

    public synchronized boolean flush() {
        InetSocketAddress address = addressReference.get();
        try {
            final int sizeOfBuffer = sendBuffer.position();

            if (sizeOfBuffer <= 0) {
                return false;
            } // empty buffer

            // send and reset the buffer
            sendBuffer.flip();
            final int nbSentBytes = channel.send(sendBuffer, address);
            sendBuffer.limit(sendBuffer.capacity());
            sendBuffer.rewind();

            if (sizeOfBuffer == nbSentBytes) {
                return true;
            } else {
                logger.log(Level.SEVERE, String.format(
                        "Could not send entirely stat %s to host %s:%d. Only sent %d bytes out of %d bytes",
                        sendBuffer.toString(),
                        address.getHostName(), address.getPort(), nbSentBytes, sizeOfBuffer));
                return false;
            }
        } catch (IOException e) {
            addressReference.purge();
            logger.log(Level.SEVERE,
                    String.format("Could not send stat %s to host %s:%d", sendBuffer.toString(), address.getHostName(),
                            address.getPort()), e);
            return false;
        }
    }

}
