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
import java.util.Objects;

import org.jmxtrans.agent.util.StringUtils2;

/**
 * @author Kristoffer Erlandsson
 */
public class InfluxMetric {

    private static final String FIELD_NAME = "value";
    private final long timestampMillis;
    private final List<InfluxTag> tags;
    private final String measurement;
    private final Object value;

    public InfluxMetric(String measurement, List<InfluxTag> tags, Object value, long timestampMillis) {
        this.measurement = Objects.requireNonNull(measurement);
        this.tags = Objects.requireNonNull(tags);
        this.value = Objects.requireNonNull(value);
        this.timestampMillis = timestampMillis;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public List<InfluxTag> getTags() {
        return tags;
    }

    public String getMeasurement() {
        return measurement;
    }

    public Object getValue() {
        return valueAsStr();
    }

    public String toInfluxFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(measurement);
        if (!tags.isEmpty()) {
            sb.append(",");
        }
        sb.append(StringUtils2.join(convertTagsToStrings(), ","))
                .append(" ")
                .append(FIELD_NAME)
                .append("=")
                .append(valueAsStr())
                .append(" ")
                .append(timestampMillis);
        return sb.toString();
    }

    private String valueAsStr() {
        if (value instanceof Integer || value instanceof Long) {
            return value.toString() + "i";
        }
        return value.toString();
    }

    private List<String> convertTagsToStrings() {
        List<String> l = new ArrayList<>(tags.size());
        for (InfluxTag influxTag : tags) {
            l.add(influxTag.toInfluxFormat());
        }
        return l;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestampMillis, tags, measurement, valueAsStr());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InfluxMetric other = (InfluxMetric) obj;
        return Objects.equals(timestampMillis, other.timestampMillis)
                && Objects.equals(tags, other.tags)
                && Objects.equals(measurement, other.measurement)
                && Objects.equals(valueAsStr(), other.valueAsStr());
    }

    @Override
    public String toString() {
        return "InfluxMetric [timestampMillis=" + timestampMillis + ", tags=" + tags + ", measurement=" + measurement
                + ", value=" + value + "]";
    }

}
