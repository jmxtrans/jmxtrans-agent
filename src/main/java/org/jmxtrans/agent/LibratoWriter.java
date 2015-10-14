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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.net.URL;
import java.net.MalformedURLException;

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

import java.net.HttpURLConnection;
import java.nio.charset.Charset;

public class LibratoWriter extends AbstractOutputWriter implements OutputWriter {
    public static final String DEFAULT_LIBRATO_API_URL = "https://metrics-api.librato.com/v1/metrics";
    public static final String SETTING_USERNAME = "username";
    public static final String SETTING_TOKEN = "token";
 
    private String user;
    private String token;
    private String basicAuthentication;
    private String httpUserAgent;
	private URL url;
	private final JsonFactory jsonFactory = new JsonFactory();

    @Override
    public synchronized void postConstruct(Map<String, String> settings) {
        user = ConfigurationUtils.getString(settings, SETTING_USERNAME);
        token = ConfigurationUtils.getString(settings, SETTING_TOKEN);
        basicAuthentication = Base64Variants.getDefaultVariant().encode((user + ":" + token).getBytes(Charset.forName("US-ASCII")));
        httpUserAgent = "jmxtrans-standalone/1 " + "(" +
						System.getProperty("java.vm.name") + "/" + System.getProperty("java.version") + "; " +
						System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "/" + System.getProperty("os.version")
						+ ")";
		try {
			url = new URL(DEFAULT_LIBRATO_API_URL);
		} catch (MalformedURLException e) {
			logger.info("Malformed url");
		}

        logger.info(String.format("LibratoWriter[username=%s, token=%s]", user, token));
    }

    @Override
    public void postCollect() throws IOException {
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public synchronized void writeQueryResult(String metricName, String metricType, Object value) {
		try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
            urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);
            urlConnection.setRequestProperty("User-Agent", httpUserAgent);
			
			JsonGenerator g = jsonFactory.createGenerator(urlConnection.getOutputStream(), JsonEncoding.UTF8);
			g.writeStartObject();
			g.writeArrayFieldStart("gauges");
			g.writeStartObject();
			g.writeStringField("name", metricName);
			if (value instanceof Integer) {
                g.writeNumberField("value", (Integer) value);
            } else if (value instanceof Long) {
                g.writeNumberField("value", (Long) value);
            } else if (value instanceof Float) {
                g.writeNumberField("value", (Float) value);
            } else if (value instanceof Double) {
                g.writeNumberField("value", (Double) value);
            }
			g.writeEndObject();
			g.writeEndArray();
        	g.writeEndObject();
        	g.flush();
        	g.close();

			int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                logger.info(String.format("Failure %d:'%s' to send result to Librato server '%s', user %s", responseCode, urlConnection.getResponseMessage(), url, user));
            }
		} catch (IOException e) {
			logger.info(String.format("Failure to send result to Librato server '%s' user %s", url, user));
		}
    }
}
