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
package org.jmxtrans.agent.graphite;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.Nullable;

/**
 * Generates metric messages to send to graphite instances.
 */
public class GraphiteMetricMessageBuilder {

    private static final String DEFAULT_METRIC_PATH_PREFIX_FORMAT = "servers.%s.";
    private final String metricPathPrefix;
    
    /**
     * @param configuredMetricPathPrefix
     *            Prefix to add to the metric keys. May be null, in which case servers.your_hostname will be used.
     */
    public GraphiteMetricMessageBuilder(@Nullable String configuredMetricPathPrefix) {
        this.metricPathPrefix = buildMetricPathPrefix(configuredMetricPathPrefix);
    } 
    
    /**
     * Builds a metric string to send to a Graphite instance.
     * 
     * @return The metric string without trailing newline
     */
    public String buildMessage(String metricName, Object value, long timestamp) {
        return metricPathPrefix + metricName + " " + convertToString(value) + " " + timestamp;
    }
    
    private String convertToString(Object value) {
    	if (value instanceof Boolean) {
            return (Boolean)value ? "1" : "0";
        }
    	return String.valueOf(value);
	}

	/**
     * {@link java.net.InetAddress#getLocalHost()} may not be known at JVM startup when the process is launched as a Linux service.
     */
    private static String buildMetricPathPrefix(String configuredMetricPathPrefix) {
        if (configuredMetricPathPrefix != null) {
            return configuredMetricPathPrefix;
        }
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName().replaceAll("\\.", "_");
        } catch (UnknownHostException e) {
            hostname = "#unknown#";
        }
        return String.format(DEFAULT_METRIC_PATH_PREFIX_FORMAT, hostname);
    }
    
    public String getPrefix() {
        return metricPathPrefix;
    }

    /**
     * Checks if the given value can be sent as a float value to graphite.
     * Note that Booleans are converted to 1/0 and will pass this check  
     * 
     * @param value value to check
     * @return true if the string representation of value is parseable as a float
     */
    public boolean isFloat(Object value) {
    	return convertToString(value).matches("[-+]?[0-9]*\\.?[0-9]+");
    }
}
