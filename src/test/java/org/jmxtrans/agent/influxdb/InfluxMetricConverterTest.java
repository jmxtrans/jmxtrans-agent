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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author Kristoffer Erlandsson
 */
public class InfluxMetricConverterTest {

    private static final ArrayList<InfluxTag> EMPTY_TAG_LIST = new ArrayList<InfluxTag>();

    @Test
    public void noExtraTags() throws Exception {
        InfluxMetric converted = InfluxMetricConverter.convertToInfluxMetric("foo", 1, EMPTY_TAG_LIST, 2l);
        InfluxMetric expected = new InfluxMetric("foo", EMPTY_TAG_LIST, 1, 2l);
        assertThat(converted, equalTo(expected));
    }

    @Test
    public void tagsInMetricName() throws Exception {
        InfluxMetric converted = InfluxMetricConverter.convertToInfluxMetric("foo,tag1=tagValue1", 1, EMPTY_TAG_LIST, 2l);
        InfluxMetric expected = new InfluxMetric("foo", Arrays.asList(new InfluxTag("tag1", "tagValue1")), 1, 2l);
        assertThat(converted, equalTo(expected));
    }

    @Test
    public void tagsWithSpacesInMetricName() throws Exception {
        InfluxMetric converted = InfluxMetricConverter.convertToInfluxMetric("foo,tag1 =tagValue1 , tag2 = tagValue2",
                1, EMPTY_TAG_LIST, 2l);
        InfluxMetric expected = new InfluxMetric("foo",
                Arrays.asList(new InfluxTag("tag1", "tagValue1"), new InfluxTag("tag2", "tagValue2")), 1, 2l);
        assertThat(converted, equalTo(expected));
    }

    @Test
    public void additionalTags() throws Exception {
        InfluxMetric converted = InfluxMetricConverter.convertToInfluxMetric("foo,tag1=tagValue1", 1,
                Arrays.asList(new InfluxTag("additionalTag", "value")), 2l);
        InfluxMetric expected = new InfluxMetric("foo",
                Arrays.asList(new InfluxTag("additionalTag", "value"), new InfluxTag("tag1", "tagValue1")), 1, 2l);
        assertThat(converted, equalTo(expected));
    }

    @Test
    public void toInfluxFormat() throws Exception {
        InfluxMetric metric = new InfluxMetric("foo",
                Arrays.asList(new InfluxTag("tag", "value"), new InfluxTag("tag2", "value2")), 1.0, 2123l);
        assertThat(metric.toInfluxFormat(), equalTo("foo,tag=value,tag2=value2 value=1.0 2123"));
    }

    @Test
    public void toInfluxFormatInteger() throws Exception {
        InfluxMetric metric = new InfluxMetric("foo", EMPTY_TAG_LIST, 1, 2123l);
        assertThat(metric.toInfluxFormat(), equalTo("foo value=1i 2123"));
    }

    @Test
    public void toInfluxFormatLong() throws Exception {
        InfluxMetric metric = new InfluxMetric("foo", EMPTY_TAG_LIST, 1l, 2123l);
        assertThat(metric.toInfluxFormat(), equalTo("foo value=1i 2123"));
    }
}
