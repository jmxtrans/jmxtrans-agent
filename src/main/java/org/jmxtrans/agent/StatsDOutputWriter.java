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

import java.nio.Buffer;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StatsDOutputWriter extends AbstractOutputWriter implements OutputWriter {

    public final static String SETTING_HOST = "host";
    public final static String SETTING_PORT = "port";
    public final static String SETTING_ROOT_PREFIX = "metricName";
    public final static String SETTING_BUFFER_SIZE = "bufferSize";
    private final static int SETTING_DEFAULT_BUFFER_SIZE = 1024;
    public final static String SETTINGS_STATSD_TYPE = "statsd";
    public final static String STATSD_DATADOG = "dd";
    public final static String STATSD_SYSDIG = "sysdig";
    public final static String SETTINGS_TAGS = "tags";
    protected List<Tag> tags;

    private ByteBuffer sendBuffer;
    protected String metricNamePrefix;
    protected String statsType;
    /**
     * Using a {@link CachingReference} instead of a raw {@link InetSocketAddress} allows to handle a change
     */
    private CachingReference<InetSocketAddress> addressReference;
    private DatagramChannel channel;

    protected synchronized void parseTagsAndMetricsNamePrefix(Map<String, String> settings) {
        statsType = ConfigurationUtils.getString(settings, SETTINGS_STATSD_TYPE, "statsd");
        if (statsType.equals(STATSD_DATADOG)) {
            String tagsStr = ConfigurationUtils.getString(settings, SETTINGS_TAGS, "");
            tags = Tag.tagsFromCommaSeparatedString(tagsStr);
            metricNamePrefix = ConfigurationUtils.getString(settings, SETTING_ROOT_PREFIX, "java");
        } else if (statsType.equals(STATSD_SYSDIG)) {
            String tagsStr = ConfigurationUtils.getString(settings, SETTINGS_TAGS, "");
            tags = Tag.tagsFromCommaSeparatedString(tagsStr, "=");
            metricNamePrefix = ConfigurationUtils.getString(settings, SETTING_ROOT_PREFIX, getHostName().replaceAll("\\.", "_"));
        }
        else {
            metricNamePrefix = ConfigurationUtils.getString(settings, SETTING_ROOT_PREFIX, getHostName().replaceAll("\\.", "_"));
        }
    }

    @Override
    public synchronized void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);

        final String host = ConfigurationUtils.getString(settings, SETTING_HOST);
        final Integer port = ConfigurationUtils.getInt(settings, SETTING_PORT);
        this.parseTagsAndMetricsNamePrefix(settings);

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

    protected synchronized String buildMetricsString(String metricName, String metricType,
        String strValue) {
        //DataDog statsd with tags (https://docs.datadoghq.com/guides/dogstatsd/),
        // metric.name:value|type|@sample_rate|#tag1:value,tag2
        //Sysdig metric tags (https://support.sysdig.com/hc/en-us/articles/204376099-Metrics-integrations-StatsD-)
        // enqueued_messages#users,country=italy:10|c
        StringBuilder sb = new StringBuilder();

        String type = "gauge".equalsIgnoreCase(metricType) || "g".equalsIgnoreCase(metricType) ? "g" : "c";
        String completeTags;
        String ddmetricName;
        if (statsType.equals(STATSD_DATADOG)) {
            if (metricName.contains("kafka.connect.task-metrics.status") || metricName.contains("kafka.connect.connector-metrics.status")) {
                String status_code;
                switch (strValue.toLowerCase()) {
                    case "running": status_code = "0"; break;
                    case "unassigned": status_code = "1"; break;
                    case "paused": status_code = "2"; break;
                    case "failed": status_code = "3"; break;
                    default: status_code = "4"; logger.warning(String.format("StatsDOutputWriter defaulted unexpected status %s to status code 4", strValue));
                }
                strValue = status_code;
            }
            if (metricName.contains("kafka.streams.client.state")) {
                // see https://github.com/apache/kafka/blob/trunk/streams/src/main/java/org/apache/kafka/streams/KafkaStreams.java#L180
                String state;
                switch (strValue.toLowerCase()) {
                    case "created": state = "0"; break;
                    case "rebalancing": state = "1"; break;
                    case "running": state = "2"; break;
                    case "pending_shutdown": state = "3"; break;
                    case "not_running": state = "4"; break;
                    case "error": state = "5"; break;
                    default: state = "6"; logger.warning(String.format("StatsDOutputWriter defaulted unexpected state %s to status code 6", strValue));
                }
                strValue = state;
            }
            if (metricName.contains("#")){
                String[] splitStr = metricName.split("#");
                List<String> metricTags = Arrays.asList(splitStr[1].split(","));
                String customTags = StringUtils2.join(metricTags, ",");
                String convertedTags = StringUtils2.join(Tag.convertTagsToStrings(tags), ",");
                completeTags = customTags + "," + convertedTags;
                ddmetricName = splitStr[0].replace("\\", "");
            } else {
                completeTags = StringUtils2.join(Tag.convertTagsToStrings(tags), ",");
                ddmetricName = metricName;
            }
            sb.append(metricNamePrefix)
                    .append(".")
                    .append(ddmetricName)
                    .append(":")
                    .append(strValue)
                    .append("|")
                    .append(type)
                    .append("|#")
                    .append(completeTags)
                    .append("\n");
        } else if (statsType.equals(STATSD_SYSDIG)) {
            sb.append(metricNamePrefix)
                    .append(".")
                    .append(metricName)
                    .append("#")
                    .append(StringUtils2.join(Tag.convertTagsToStrings(tags), ","))
                    .append(":")
                    .append(strValue)
                    .append("|")
                    .append(type)
                    .append("\n");
        } else {
            sb.append(metricNamePrefix)
                    .append(".")
                    .append(metricName)
                    .append(":")
                    .append(strValue)
                    .append("|")
                    .append(type)
                    .append("\n");
        }
        return sb.toString();
    }

    @Override
    public synchronized void writeQueryResult(String metricName, String metricType, Object value) throws IOException
    {
        // statsd expects a number value for the metric.
        //
        // skip if value's string representation equals to "NaN" or "INF", which are meaningless values to statsd.
        // passing the invalid values down will trigger error in downstream parsing applications.
        String strValue = String.valueOf(value);
        if (strValue.equals("NaN") || strValue.equals("INF")) {
            return;
        }
        String outputMessage = this.buildMetricsString(metricName, metricType, strValue);

        if (logger.isLoggable(getDebugLevel())) {
            logger.log(getDebugLevel(), "Sending msg: " + outputMessage);
        }
        doSend(outputMessage);
    }

    protected synchronized boolean doSend(String stat) {
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
            ((Buffer)sendBuffer).flip();
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
        } catch (IOException | RuntimeException e) { // RuntimeException can by BufferOverflowException...
            addressReference.purge();
            logger.log(Level.SEVERE,
                    String.format("Could not send stat %s to host %s:%d", sendBuffer.toString(), address.getHostName(),
                            address.getPort()), e);
            sendBuffer.clear();
            return false;
        }
    }

}
