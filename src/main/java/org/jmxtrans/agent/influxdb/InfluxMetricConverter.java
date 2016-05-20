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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kristoffer Erlandsson
 */
public class InfluxMetricConverter {

    public static InfluxMetric convertToInfluxMetric(String metricName, Object value, List<InfluxTag> additionalTags, long timestamp) {
        List<InfluxTag> tagsFromMetricName = parseTags(metricName);
        List<InfluxTag> allTags = new ArrayList<>(additionalTags);
        allTags.addAll(tagsFromMetricName);
        return new InfluxMetric(parseMeasurement(metricName), allTags, value, timestamp);
    }

    private static String parseMeasurement(String metricName) {
        return metricName.split(",")[0].trim();
    }

    private static List<InfluxTag> parseTags(String metricName) {
        int startOfTags = metricName.indexOf(',');
        if (startOfTags < 0) {
            return new ArrayList<>();
        }
        return tagsFromCommaSeparatedString(metricName.substring(startOfTags + 1));
    }

    public static List<InfluxTag> tagsFromCommaSeparatedString(String s) {
        List<InfluxTag> tags = new ArrayList<>();
        if (s.trim().isEmpty()) {
            return tags;
        }
        String[] parts = s.split(",");
        for (String tagPart : parts) {
            tags.add(parseOneTag(tagPart));
        }
        return tags;
    }

    private static InfluxTag parseOneTag(String part) {
        String[] nameAndValue = part.trim().split("=");
        if (nameAndValue.length != 2) {
            throw new FailedToConvertToInfluxMetricException(
                    "Error when parsing influx tags from substring " + part + ", must be on format <name>=<value>,...");
        }
        InfluxTag tag = new InfluxTag(nameAndValue[0].trim(), nameAndValue[1].trim());
        return tag;
    }

    @SuppressWarnings("serial")
    public static class FailedToConvertToInfluxMetricException extends RuntimeException {

        public FailedToConvertToInfluxMetricException(String msg) {
            super(msg);
        }

    }
}
