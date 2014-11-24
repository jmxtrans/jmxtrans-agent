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

import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.util.io.IoUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://graphite.readthedocs.org/">Graphite</a> implementation of the {@linkplain OutputWriter}.
 * <p/>
 * This implementation uses <a href="http://graphite.readthedocs.org/en/0.9.10/feeding-carbon.html#the-plaintext-protocol">
 * Carbon Plan Text protocol</a> over HTTP.
 * Read <a href="https://answers.launchpad.net/graphite/+question/213436">this thread</a> in order to implement a HTTP
 * listener in front of Carbon backend.
 * <p/>
 * Settings:
 * <ul>
 * <li>"url": complete url of the Graphite proxy server. Mandatory</li>
 * <li>"namePrefix": prefix append to the metrics name.
 * Optional, default value: {@value #DEFAULT_NAME_PREFIX}.</li>
 * </ul>
 *
 * @author <a href="mailto:simon.mazas@gmail.com">Simon Mazas</a>
 */
public class GraphiteHttpWriter extends AbstractOutputWriter implements OutputWriter {

    public static final String DEFAULT_NAME_PREFIX = "servers.#hostname#.";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Metric path prefix. Ends with "." if not empty;
     */
    private String metricPathPrefix;

    private URL graphiteHttpUrl;

    /**
     * Load settings, initialize the {@link org.apache.commons.httpclient .HttpClient.HttpClient} client and test the connection to the graphite server.
     * <p/>
     * a {@link Logger#warn(String)} message is emitted if the connection to the graphite server fails.
     */
    @Override
    public void start() {
        String url = getStringSetting(SETTING_URL);

        try {
            graphiteHttpUrl = new URL(url);
        } catch (MalformedURLException e) {
            throw new EmbeddedJmxTransException(e);
        }

	    logger.info("Start Graphite writer connected to '{}'...", graphiteHttpUrl);

        metricPathPrefix = getStringSetting(SETTING_NAME_PREFIX, DEFAULT_NAME_PREFIX);
        metricPathPrefix = getStrategy().resolveExpression(metricPathPrefix);
        if (!metricPathPrefix.isEmpty() && !metricPathPrefix.endsWith(".")) {
            metricPathPrefix = metricPathPrefix + ".";
        }

    }

    /**
     * Send given metrics to the Graphite server.
     */
    @Override
    public void write(Iterable<QueryResult> results) {
        logger.debug("Export to '{}' results {}", graphiteHttpUrl, results);
        HttpURLConnection urlConnection = null;
        OutputStreamWriter urlWriter;
        try {
            StringBuilder sbUrlWriter = new StringBuilder("");
            for (QueryResult result : results) {
                String msg = metricPathPrefix + result.getName() + " " + result.getValue() + " " + result.getEpoch(TimeUnit.SECONDS) + "\n";
                logger.debug("Export '{}'", msg);
                sbUrlWriter.append(msg);
            }
            if (sbUrlWriter.length() > 0) {
                sbUrlWriter.insert(0, "data=");
                urlConnection = (HttpURLConnection) graphiteHttpUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlWriter = new OutputStreamWriter(urlConnection.getOutputStream(), Charset.forName("UTF-8"));
                urlWriter.write(sbUrlWriter.toString());
                urlWriter.flush();
                IoUtils2.closeQuietly(urlWriter);
                int responseCode = urlConnection.getResponseCode();
                if (responseCode != 200) {
                    logger.warn("Failure {}:'{}' to send result to Graphite HTTP proxy'{}' ", responseCode, urlConnection.getResponseMessage(), graphiteHttpUrl);
                }
                if (logger.isTraceEnabled()) {
                    IoUtils2.copy(urlConnection.getInputStream(), System.out);
                }
            }
        } catch (Exception e) {
            logger.warn("Failure to send result to Graphite HTTP proxy '{}'", graphiteHttpUrl, e);
        } finally {
            // Release the connection.
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

}

