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


import org.jmxtrans.agent.util.ConfigurationUtils;
import org.jmxtrans.agent.util.StandardCharsets2;
import org.jmxtrans.agent.util.StringUtils2;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * OutputWriter for <a href="https://www.librato.com/">Librato</a>.
 */
public class LibratoWriter extends AbstractOutputWriter implements OutputWriter {
    public static final String DEFAULT_LIBRATO_API_URL = "https://metrics-api.librato.com/v1/metrics";
    public static final String SETTING_USERNAME = "username";
    public static final String SETTING_TOKEN = "token";

    private String username;
    private String basicAuthentication;
    private String httpUserAgent;
    private URL url;

    private final AtomicInteger exceptionCounter = new AtomicInteger();

    @Override
    public synchronized void postConstruct(Map<String, String> settings) {
        final String username = ConfigurationUtils.getString(settings, SETTING_USERNAME);
        final String token = ConfigurationUtils.getString(settings, SETTING_TOKEN);
        if (username == null || token == null) {
            throw new RuntimeException("Username and/or token cannot be null");
        }
        this.username = username;
        basicAuthentication = DatatypeConverter.printBase64Binary((username + ":" + token).getBytes(StandardCharsets2.US_ASCII));
        httpUserAgent = "jmxtrans-agent/2 " + "(" +
                System.getProperty("java.vm.name") + "/" + System.getProperty("java.version") + "; " +
                System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "/" + System.getProperty("os.version")
                + ")";
        try {
            url = new URL(DEFAULT_LIBRATO_API_URL);
        } catch (MalformedURLException e) {
            logger.info("Malformed url");
        }

        logger.info(String.format("LibratoWriter[username=%s, token=***]", username));
    }

    @Override
    public void postCollect() throws IOException {
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public synchronized void writeQueryResult(String metricName, String metricType, Object value) throws IOException {
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
            urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);
            urlConnection.setRequestProperty("User-Agent", httpUserAgent);

            Writer out = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets2.UTF_8);

            writeQueryResult(metricName, metricType, value, out);
            out.flush();
            out.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                exceptionCounter.incrementAndGet();
                logger.info(String.format("Failure %d:'%s' to send result to Librato server '%s', username %s", responseCode, urlConnection.getResponseMessage(), url, username));
            }
        } catch (RuntimeException e) {
            exceptionCounter.incrementAndGet();
            logger.info(String.format("Failure to send result to Librato server '%s' username %s", url, username));
        }
    }

    protected void writeQueryResult(String metricName, String metricType, Object value, Writer out) throws IOException {
        String libratoMetricsType = "gauge".equalsIgnoreCase(metricType) || "g".equalsIgnoreCase(metricType) ? "gauges" : "counters";

        out.write("{");
        out.write("\"" + libratoMetricsType + "\": [");
        out.write("{");
        out.write("\"name\": \"" + metricName + "\"");
        out.write(",");
        out.write("\"value\":");
        if (value == null) {
            out.write("null");
        } else if (value instanceof Number) {
            out.write(value.toString());
        } else if (value instanceof String) {
            out.write("\"" + ((String) value).replaceAll("\"", "\\\"") + "\"");
        } else {
            String valueAsString = value.toString();
            if (logger.isLoggable(Level.FINE)) {
                logger.warning("warning: no converter found for metric \"" + metricName + "\" with value type \"" + value.getClass() + "\". Use toString() on \"" + StringUtils2.abbreviate(valueAsString, 20) + "\"");
            }
            out.write(valueAsString);
        }
        out.write("}");
        out.write("]");
        out.write("}");
    }

    public int getExceptionCounter() {
        return exceptionCounter.get();
    }

}
