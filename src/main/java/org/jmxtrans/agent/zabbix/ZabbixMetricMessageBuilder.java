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
package org.jmxtrans.agent.zabbix;

import javax.annotation.Nullable;

import org.jmxtrans.agent.util.json.JsonObject;

/**
 * Generates metric messages to send to graphite instances.
 */
public class ZabbixMetricMessageBuilder {

    private final String hostName;
    
    /**
     * @param configuredMetricPathPrefix
     *            Prefix to add to the metric keys. May be null, in which case servers.your_hostname will be used.
     */
    public ZabbixMetricMessageBuilder(@Nullable String hostName) {
        this.hostName = hostName;
    } 
    
    public String getHostName()
    {
        return hostName;
    }
    
    /**
     * Builds a metric string to send to a Graphite instance.
     * 
     * @return The metric string without trailing newline
     */
    public String buildMessage(String metricName, Object value, long timestamp) {
        String valueToString = "null";
        if( value != null ) {
            valueToString = value.toString();
        }

        JsonObject jsonValue = new JsonObject();
        jsonValue.add("host", getHostName());
        jsonValue.add("key", metricName);
        jsonValue.add("value", valueToString);
        jsonValue.add("clock", timestamp);

        return jsonValue.toString();
    }
    
        
}
