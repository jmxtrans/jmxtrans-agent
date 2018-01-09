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
package org.jmxtrans.agent.influxdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jmxtrans.agent.AbstractOutputWriter;
import org.jmxtrans.agent.util.ConfigurationUtils;
import org.jmxtrans.agent.util.StandardCharsets2;
import org.jmxtrans.agent.util.io.IoRuntimeException;
import org.jmxtrans.agent.util.io.IoUtils;
import org.jmxtrans.agent.util.time.Clock;
import org.jmxtrans.agent.util.time.SystemCurrentTimeMillisClock;

import static org.jmxtrans.agent.util.ConfigurationUtils.getBoolean;

/**
 * Output writer for InfluxDb.
 * 
 * @author Kristoffer Erlandsson
 */
public class InfluxDbOutputWriter extends AbstractOutputWriter {

    private URL url;
    private String database;
    private String user; // Null if not configured
    private String password; // Null if not configured 
    private String retentionPolicy; // Null if not configured
    private List<InfluxTag> tags;
    private List<InfluxMetric> batchedMetrics = new ArrayList<>();
    private int connectTimeoutMillis;
    private int readTimeoutMillis;
    private final Clock clock;
    private boolean enabled;
    public final static String SETTING_ENABLED = "enabled";

    public InfluxDbOutputWriter() {
        this.clock = new SystemCurrentTimeMillisClock();
    }

    /**
     * Test hook for supplying a fake clock.
     */
    InfluxDbOutputWriter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void postConstruct(Map<String, String> settings) {
        enabled = getBoolean(settings, SETTING_ENABLED, true);
        String urlStr = ConfigurationUtils.getString(settings, "url");
        database = ConfigurationUtils.getString(settings, "database");
        user = ConfigurationUtils.getString(settings, "user", null);
        password = ConfigurationUtils.getString(settings, "password", null);
        retentionPolicy = ConfigurationUtils.getString(settings, "retentionPolicy", null);
        String tagsStr = ConfigurationUtils.getString(settings, "tags", "");
        tags = InfluxMetricConverter.tagsFromCommaSeparatedString(tagsStr);
        connectTimeoutMillis = ConfigurationUtils.getInt(settings, "connectTimeoutMillis", 3000);
        readTimeoutMillis = ConfigurationUtils.getInt(settings, "readTimeoutMillis", 5000);
        url = parseUrlStr(getWriteEndpointForUrlStr(urlStr));
        logger.log(getInfoLevel(), "InfluxDbOutputWriter is configured with url=" + urlStr
                + ", database=" + database
                + ", user=" + user
                + ", password=" + (password != null ? "****" : null)
                + ", tags=" + tagsStr
                + ", connectTimeoutMills=" + connectTimeoutMillis
                + ", readTimeoutMillis=" + readTimeoutMillis);
    }

    private String getWriteEndpointForUrlStr(String urlStr) {
        return urlStr + (urlStr.endsWith("/") ? "write" : "/write");
    }

    /**
     *
     * @param urlStr
     * @return url composed with the query string
     */
    private URL parseUrlStr(String urlStr) {
        try {
            // TODO shouldn't we check if it is "?" or "&" according to the existence of a querystring part in the configuration URL?
            return new URL(urlStr + "?" + buildQueryString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQueryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("precision=ms")
                .append("&db=").append(database);
        appendParamIfNotEmptyOrNull(sb, "u", user);
        appendParamIfNotEmptyOrNull(sb, "p", password);
        appendParamIfNotEmptyOrNull(sb, "rp", retentionPolicy);
        return sb.toString();
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        if(!enabled) return;
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public void writeQueryResult(String metricName, String metricType, Object value) throws IOException {
        if(!enabled) return;
        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(metricName, value, tags,
                clock.getCurrentTimeMillis());
        batchedMetrics.add(metric);
    }

    @Override
    public void postCollect() throws IOException {
        if(!enabled) return;
        String body = convertMetricsToLines(batchedMetrics);
        if (logger.isLoggable(getTraceLevel())) {
            logger.log(getTraceLevel(), "Sending to influx (" + url + "):\n" + body);
        }
        batchedMetrics.clear();
        sendMetrics(body);
    }

    private void sendMetrics(String body) throws IOException {
        HttpURLConnection conn = createAndConfigureConnection();
        try {
            sendMetrics(body, conn);
        } catch (IOException e) {
            throw new IOException("Exception sending metrics to '" + conn.getURL() + "': " + e.toString(), e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Exception sending metrics to '" + conn.getURL() + "': " + e.toString(), e);
        } finally {
            IoUtils.closeQuietly(conn);
        }
    }

    private void sendMetrics(String body, HttpURLConnection conn) throws IOException {
        writeMetrics(conn, body);
        int responseCode = conn.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new RuntimeException("Failed to write metrics, response code: " + responseCode
                    + ", response message: " + conn.getResponseMessage());
        }
        String response = readResponse(conn);
        if (logger.isLoggable(getTraceLevel())) {
            logger.log(getTraceLevel(), "Response from influx: " + response);
        }
    }

    private HttpURLConnection createAndConfigureConnection() throws ProtocolException {
        HttpURLConnection conn = openHttpConnection();
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        return conn;
    }

    private HttpURLConnection openHttpConnection() {
        try {
            return (HttpURLConnection) url.openConnection();
        } catch (IOException | ClassCastException e) {
            throw new IoRuntimeException("Failed to create HttpURLConnection to '" + url + "' - is it a valid HTTP url?",
                    e);
        }
    }

    private void writeMetrics(HttpURLConnection conn, String body)
            throws IOException {
        byte[] toSendBytes = body.getBytes(StandardCharsets2.UTF_8);
        conn.setRequestProperty("Content-Length", Integer.toString(toSendBytes.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(toSendBytes);
            os.flush();
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = conn.getInputStream()) {
            IoUtils.copy(is, baos);
        }
        String response = new String(baos.toByteArray(), "UTF-8");
        return response;
    }

    private void appendParamIfNotEmptyOrNull(StringBuilder sb, String paramName, String paramValue) {
        if (paramValue != null && !paramValue.trim().isEmpty()) {
            // NB: We do not URL encode anything, from what I understand from the Influx docs,
            // encoded data is not expected.
            sb.append("&").append(paramName).append("=").append(paramValue);
        }

    }

    private String convertMetricsToLines(List<InfluxMetric> metrics) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<InfluxMetric> it = metrics.iterator(); it.hasNext();) {
            InfluxMetric metric = it.next();
            sb.append(metric.toInfluxFormat());
            if (it.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}
