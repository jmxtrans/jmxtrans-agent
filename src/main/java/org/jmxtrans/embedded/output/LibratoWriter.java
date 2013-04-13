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
package org.jmxtrans.embedded.output;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.util.io.IoUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <a href="https://metrics.librato.com//">Librato Metrics</a> implementation of the {@linkplain org.jmxtrans.embedded.output.OutputWriter}.
 * <p/>
 * This implementation uses <a href="http://dev.librato.com/v1/post/metrics">
 * POST {@code /v1/metrics}</a> HTTP API.
 * <p/>
 * {@link LibratoWriter} uses the "{@code query.attribute.type}" configuration parameter (via
 * {@link org.jmxtrans.embedded.QueryResult#getType()}) to publish the metrics.<br/>
 * Supported types are {@value #METRIC_TYPE_COUNTER} and {@value #METRIC_TYPE_GAUGE}.<br/>
 * If the type is <code>null</code> or unsupported, metric is exported
 * as {@value #METRIC_TYPE_COUNTER}.
 * <p/>
 * Settings:
 * <ul>
 * <li>"{@code url}": Librato server URL.
 * Optional, default value: {@value #DEFAULT_LIBRATO_API_URL}.</li>
 * <li>"{@code user}": Librato user. Mandatory</li>
 * <li>"{@code token}": Librato token. Mandatory</li>
 * <li>"{@code libratoApiTimeoutInMillis}": read timeout of the calls to Librato HTTP API.
 * Optional, default value: {@value #DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS}.</li>
 * <li>"{@code enabled}": flag to enable/disable the writer. Optional, default value: <code>true</code>.</li>
 * <li>"{@code source}": Librato . Optional, default value: {@value #DEFAULT_SOURCE} (the hostname of the server).</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class LibratoWriter extends AbstractOutputWriter implements OutputWriter {

    public static final String METRIC_TYPE_GAUGE = "gauge";
    public static final String METRIC_TYPE_COUNTER = "counter";
    public static final String DEFAULT_LIBRATO_API_URL = "https://metrics-api.librato.com/v1/metrics";
    public static final String SETTING_LIBRATO_API_TIMEOUT_IN_MILLIS = "libratoApiTimeoutInMillis";
    public static final int DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS = 1000;
    public static final String SETTING_SOURCE = "source";
    public static final String DEFAULT_SOURCE = "#hostname#";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicInteger exceptionCounter = new AtomicInteger();
    private JsonFactory jsonFactory = new JsonFactory();
    /**
     * Librato HTTP API URL
     */
    private URL url;
    private int libratoApiTimeoutInMillis = DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS;
    /**
     * Librato HTTP API authentication username
     */
    private String user;
    /**
     * Librato HTTP API authentication token
     */
    private String token;
    private String basicAuthentication;
    /**
     * Optional proxy for the http API calls
     */
    @Nullable
    private Proxy proxy;
    /**
     * Librato measurement property 'source',
     */
    @Nullable
    private String source;

    /**
     * Load settings<p/>
     */
    @Override
    public void start() {
        try {
            url = new URL(getStringSetting(SETTING_URL, DEFAULT_LIBRATO_API_URL));

            user = getStringSetting(SETTING_USERNAME);
            token = getStringSetting(SETTING_TOKEN);
            basicAuthentication = Base64Variants.getDefaultVariant().encode((user + ":" + token).getBytes(Charset.forName("US-ASCII")));

            if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST).isEmpty()) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST), getIntSetting(SETTING_PROXY_PORT)));
            }

            libratoApiTimeoutInMillis = getIntSetting(SETTING_LIBRATO_API_TIMEOUT_IN_MILLIS, DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS);

            source = getStringSetting(SETTING_SOURCE, DEFAULT_SOURCE);
            source = getStrategy().resolveExpression(source);

            logger.info("Start Librato writer connected to '{}', proxy {} with user '{}' ...", url, proxy, user);
        } catch (MalformedURLException e) {
            throw new EmbeddedJmxTransException(e);
        }
    }

    /**
     * Send given metrics to the Graphite server.
     */
    @Override
    public void write(Iterable<QueryResult> results) {
        logger.debug("Export to '{}', proxy {} metrics {}", url, proxy, results);

        List<QueryResult> counters = new ArrayList<QueryResult>();
        List<QueryResult> gauges = new ArrayList<QueryResult>();
        for (QueryResult result : results) {
            if (METRIC_TYPE_GAUGE.equals(result.getType())) {
                gauges.add(result);
            } else if (METRIC_TYPE_COUNTER.equals(result.getType())) {
                counters.add(result);
            } else if (null == result.getType()) {
                logger.info("Unspecified type for result {}, export it as counter");
                counters.add(result);
            } else {
                logger.info("Unsupported metric type '{}' for result {}, export it as counter", result.getType(), result);
                counters.add(result);
            }
        }


        HttpURLConnection urlConnection = null;
        try {
            if (proxy == null) {
                urlConnection = (HttpURLConnection) url.openConnection();
            } else {
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
            }
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(libratoApiTimeoutInMillis);
            urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
            urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);

            serialize(counters, gauges, urlConnection.getOutputStream());
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                exceptionCounter.incrementAndGet();
                logger.warn("Failure {}:'{}' to send result to Librato server '{}' with proxy {}, user {}", responseCode, urlConnection.getResponseMessage(), url, proxy, user);
            }
            if (logger.isTraceEnabled()) {
                IoUtils2.copy(urlConnection.getInputStream(), System.out);
            }
        } catch (Exception e) {
            exceptionCounter.incrementAndGet();
            logger.warn("Failure to send result to Librato server '{}' with proxy {}, user {}", url, proxy, user, e);
        } finally {
            if (urlConnection != null) {
                try {
                    InputStream in = urlConnection.getInputStream();
                    IoUtils2.copy(in, IoUtils2.nullOutputStream());
                    IoUtils2.closeQuietly(in);
                    InputStream err = urlConnection.getErrorStream();
                    if (err != null) {
                        IoUtils2.copy(err, IoUtils2.nullOutputStream());
                        IoUtils2.closeQuietly(err);
                    }
                } catch (IOException e) {
                    logger.warn("Exception flushing http connection", e);
                }
            }

        }
    }

    public void serialize(@Nonnull Iterable<QueryResult> counters, @Nonnull Iterable<QueryResult> gauges, @Nonnull OutputStream out) throws IOException {
        JsonGenerator g = jsonFactory.createGenerator(out, JsonEncoding.UTF8);
        g.writeStartObject();
        g.writeArrayFieldStart("counters");

        for (QueryResult counter : counters) {
            g.writeStartObject();
            g.writeStringField("name", counter.getName());
            if (source != null && !source.isEmpty()) {
                g.writeStringField("source", source);
            }
            g.writeNumberField("measure_time", counter.getEpoch(TimeUnit.SECONDS));
            if (counter.getValue() instanceof Integer) {
                g.writeNumberField("value", (Integer) counter.getValue());
            } else if (counter.getValue() instanceof Long) {
                g.writeNumberField("value", (Long) counter.getValue());
            } else if (counter.getValue() instanceof Float) {
                g.writeNumberField("value", (Float) counter.getValue());
            } else if (counter.getValue() instanceof Double) {
                g.writeNumberField("value", (Double) counter.getValue());
            }
            g.writeEndObject();
        }
        g.writeEndArray();

        g.writeArrayFieldStart("gauges");

        for (QueryResult gauge : gauges) {
            g.writeStartObject();
            g.writeStringField("name", gauge.getName());
            if (source != null && !source.isEmpty()) {
                g.writeStringField("source", source);
            }
            g.writeNumberField("measure_time", gauge.getEpoch(TimeUnit.SECONDS));
            if (gauge.getValue() instanceof Integer) {
                g.writeNumberField("value", (Integer) gauge.getValue());
            } else if (gauge.getValue() instanceof Long) {
                g.writeNumberField("value", (Long) gauge.getValue());
            } else if (gauge.getValue() instanceof Float) {
                g.writeNumberField("value", (Float) gauge.getValue());
            } else if (gauge.getValue() instanceof Double) {
                g.writeNumberField("value", (Double) gauge.getValue());
            }
            g.writeEndObject();
        }
        g.writeEndArray();
        g.writeEndObject();
        g.flush();
        g.close();
    }

    public int getExceptionCounter() {
        return exceptionCounter.get();
    }
}
