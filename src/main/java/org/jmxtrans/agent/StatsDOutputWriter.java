package org.jmxtrans.agent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.logging.Level;

import org.jmxtrans.agent.util.ConfigurationUtils;

public class StatsDOutputWriter extends AbstractOutputWriter implements OutputWriter {

    private final static String SETTING_HOST = "host";
    private final static String SETTING_PORT = "port";
    private final static String SETTING_ROOT_PREFIX = "metricName";
    private final static String SETTING_BUFFER_SIZE = "bufferSize";
    private final static int SETTING_DEFAULT_BUFFER_SIZE = 1024;
    private final static String PATTERN = "%s.%s:%s|c\n";

    private ByteBuffer sendBuffer;
    private String metricNamePrefix;
    private InetSocketAddress address;
    private DatagramChannel channel;

    @Override
    public void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);

        String host = ConfigurationUtils.getString(settings, SETTING_HOST);
        Integer port = ConfigurationUtils.getInt(settings, SETTING_PORT);
        metricNamePrefix = getMetricNameOrDefaultValue(settings);

        if (port == null || nullOrEmpty(host)) {
            throw new RuntimeException("Host and/or port cannot be null");
        }

        int bufferSize = ConfigurationUtils.getInt(settings, SETTING_BUFFER_SIZE, SETTING_DEFAULT_BUFFER_SIZE);
        sendBuffer = ByteBuffer.allocate(bufferSize);

        try {
            address = new InetSocketAddress(host, port);
            channel = DatagramChannel.open();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to open channel");
            throw new RuntimeException(e);
        }

        logger.info(String.format("StatsDOutputWriter[host=%s, port=%d, metricNamePrefix=%s]", host, port,
                metricNamePrefix));
    }

    private String getMetricNameOrDefaultValue(Map<String, String> settings) {
        String metricName = ConfigurationUtils.getString(settings, SETTING_ROOT_PREFIX);

        if (nullOrEmpty(metricName)) {
            return getHostName().replaceAll("\\.", "_");
        } else {
            return metricName;
        }
    }

    private String getHostName() {
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

    private boolean nullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
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
        String stats = String.format(PATTERN, metricNamePrefix, metricName, value);
        logger.log(getDebugLevel(), "Sending msg: " + stats);
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

            sendBuffer.put(data); // append the data
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format(
                    "Could not send stat %s to host %s:%d", sendBuffer.toString(), address.getHostName(),
                    address.getPort()), e);
            return false;
        }
    }

    public synchronized boolean flush() {
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
            logger.log(Level.SEVERE,
                    String.format("Could not send stat %s to host %s:%d", sendBuffer.toString(), address.getHostName(),
                            address.getPort()), e);
            return false;
        }
    }

}
