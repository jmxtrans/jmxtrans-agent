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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.util.io.IoUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * <a href="https://www.stackdriver.com//">Stackdriver</a> implementation of the
 * {@linkplain org.jmxtrans.embedded.output.OutputWriter}.
 * <p/>
 * This implementation uses <a href="https://custom-gateway.stackdriver.com/v1/custom"> POST {@code /v1/metrics}</a>
 * HTTP API.
 * <p/>
 * Settings:
 * <ul>
 * <li>"{@code url}": Stackdriver server URL. Optional, default value: {@value #DEFAULT_STACKDRIVER_API_URL}.</li>
 * <li>"{@code token}": Stackdriver API token. Mandatory</li>
 * <li>"{@code source}": Instance of the machine ID that the JMX data is being collected from. Optional.
 * <li>"{@code detectInstance}": Set to "AWS" if you want to detect the local AWS instance ID on startup.  Optional. 
 * <li>"{@code stackdriverApiTimeoutInMillis}": read timeout of the calls to Stackdriver HTTP API. Optional, default
 * value: {@value #DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS}.</li>
 * <li>"{@code enabled}": flag to enable/disable the writer. Optional, default value: <code>true</code>.</li>
 * </ul>
 * 
 * @author <a href="mailto:eric@stackdriver.com">Eric Kilby</a>
 */
public class StackdriverWriter extends AbstractOutputWriter implements OutputWriter {

	public static final int STACKDRIVER_PROTOCOL_VERSION = 1;
	public static final String SETTING_SOURCE_INSTANCE = "source";
	public static final String SETTING_DETECT_INSTANCE = "detectInstance";
	public static final String DEFAULT_STACKDRIVER_API_URL = "https://custom-gateway.stackdriver.com/v1/custom";
	public static final String SETTING_STACKDRIVER_API_TIMEOUT_IN_MILLIS = "stackdriverApiTimeoutInMillis";
	public static final int DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS = 1000;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final AtomicInteger exceptionCounter = new AtomicInteger();
	private JsonFactory jsonFactory = new JsonFactory();
	/**
	 * Stackdriver HTTP API URL
	 */
	private URL url;
	private int stackdriverApiTimeoutInMillis = DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS;

	/**
	 * Stackdriver API key
	 */
	private String apiKey;

	/**
	 * Optional proxy for the http API calls
	 */
	@Nullable
	private Proxy proxy;

	/**
	 * AWS instance ID, populated on startup
	 */
    @Nullable
	private String instanceId;

	/**
	 * Initial setup for the writer class. Loads in settings and initializes one-time setup variables like instanceId.
	 */
	@Override
	public void start() {
		try {
			url = new URL(getStringSetting(SETTING_URL, DEFAULT_STACKDRIVER_API_URL));
		} catch (MalformedURLException e) {
			throw new EmbeddedJmxTransException(e);
		}

		apiKey = getStringSetting(SETTING_TOKEN);

		if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST).isEmpty()) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST), getIntSetting(SETTING_PROXY_PORT)));
		}
		
		logger.info("Starting Stackdriver writer connected to '{}', proxy {} ...", url, proxy);
		

		stackdriverApiTimeoutInMillis = getIntSetting(SETTING_STACKDRIVER_API_TIMEOUT_IN_MILLIS, DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS);

		// try to get and instance ID
		if (getStringSetting(SETTING_SOURCE_INSTANCE, null) != null && !getStringSetting(SETTING_SOURCE_INSTANCE).isEmpty()) {
			// if one is set directly use that
			instanceId = getStringSetting(SETTING_SOURCE_INSTANCE);
			logger.info("Using instance ID {} from setting {}", instanceId, SETTING_SOURCE_INSTANCE);
		} else if (getStringSetting(SETTING_DETECT_INSTANCE, null) != null && "AWS".equalsIgnoreCase(getStringSetting(SETTING_DETECT_INSTANCE))) {
			// if setting is to detect, look on the local machine URL
			logger.info("Detect instance set to AWS, trying to determine AWS instance ID");
			instanceId = getLocalAwsInstanceId();
			if (instanceId != null) {
				logger.info("Detected instance ID as {}", instanceId);
			} else {
				logger.info("Unable to detect AWS instance ID for this machine, sending metrics without an instance ID");
			}
		} else {
			// no instance ID, the metrics will be sent as "bare" custom metrics and not associated with an instance
			instanceId = null;
			logger.info("No source instance ID passed, and not set to detect, sending metrics without and instance ID");
		}
		
	}

	/**
	 * Send given metrics to the Stackdriver server using HTTP
	 * 
	 * @param results
	 *            Iterable collection of data points
	 */
	@Override
	public void write(Iterable<QueryResult> results) {
		logger.debug("Export to '{}', proxy {} metrics {}", url, proxy, results);

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
			urlConnection.setReadTimeout(stackdriverApiTimeoutInMillis);
			urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
			urlConnection.setRequestProperty("x-stackdriver-apikey", apiKey);

			serialize(results, urlConnection.getOutputStream());
			int responseCode = urlConnection.getResponseCode();
			if (responseCode != 200 && responseCode != 201) {
				exceptionCounter.incrementAndGet();
				logger.warn("Failure {}:'{}' to send result to Stackdriver server '{}' with proxy {}", responseCode,
						urlConnection.getResponseMessage(), url, proxy);
			}
			if (logger.isTraceEnabled()) {
				IoUtils2.copy(urlConnection.getInputStream(), System.out);
			}
		} catch (Exception e) {
			exceptionCounter.incrementAndGet();
			logger.warn("Failure to send result to Stackdriver server '{}' with proxy {}", url, proxy, e);
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
					urlConnection.disconnect();
				} catch (IOException e) {
					logger.warn("Error flushing http connection for one result, continuing");
					logger.debug("Stack trace for the http connection, usually a network timeout", e);
				}
			}

		}
	}

	/**
	 * Use the EC2 Metadata URL to determine the instance ID that this code is running on. Useful if you don't want to
	 * configure the instance ID manually. Pass detectInstance = "AWS" to have this run in your configuration.
	 * 
	 * @return String containing an AWS instance id, or null if none is found
	 */
    @Nullable
	private String getLocalAwsInstanceId() {
		String detectedInstanceId = null;
		try {
			final URL metadataUrl = new URL("http://169.254.169.254/latest/meta-data/instance-id");
            URLConnection metadataConnection = metadataUrl.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(metadataConnection.getInputStream(), "UTF-8"));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
				detectedInstanceId = inputLine;
			}
			in.close();
		} catch (Exception e) {
			logger.warn("unable to determine AWS instance ID", e);
		}
		return detectedInstanceId;
	}

	/**
	 * Put the values into the JSON format expected by the Stackdriver custom metrics gateway
	 *
     * @param results
     *            Iterable collection of data points (gauges and counters)
	 * @param out
	 *            OutputStream to write JSON to
	 * @throws IOException
	 */
	public void serialize(@Nonnull Iterable<QueryResult> results, @Nonnull OutputStream out) throws IOException {
		JsonGenerator g = jsonFactory.createGenerator(out, JsonEncoding.UTF8);
		g.writeStartObject();
		g.writeNumberField("timestamp", System.currentTimeMillis() / 1000);
		g.writeNumberField("proto_version", STACKDRIVER_PROTOCOL_VERSION);
		g.writeArrayFieldStart("data");

		for (QueryResult metric : results) {
			g.writeStartObject();
			g.writeStringField("name", metric.getName());
			if (instanceId != null && !instanceId.isEmpty()) {
				g.writeStringField("instance", instanceId);
			}
			g.writeNumberField("collected_at", metric.getEpoch(TimeUnit.SECONDS));
			if (metric.getValue() instanceof Integer) {
				g.writeNumberField("value", (Integer) metric.getValue());
			} else if (metric.getValue() instanceof Long) {
				g.writeNumberField("value", (Long) metric.getValue());
			} else if (metric.getValue() instanceof Float) {
				g.writeNumberField("value", (Float) metric.getValue());
			} else if (metric.getValue() instanceof Double) {
				g.writeNumberField("value", (Double) metric.getValue());
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
