package org.jmxtrans.agent;

/*
 * Copyright (c) 2010-2015 the original author or authors
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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;

public class StatsDOutputWriterTest {
    StatsDOutputWriterMock writer = new StatsDOutputWriterMock();


    @Test
    public void test_write_counter_metric() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put(StatsDOutputWriter.SETTING_ROOT_PREFIX, "foo.bar");
        // No real connect is done. Config is here to please the postConstruct.
        settings.put(StatsDOutputWriter.SETTING_HOST, "localhost");
        settings.put(StatsDOutputWriter.SETTING_PORT, "8125");

        writer.postConstruct(settings);
        writer.writeQueryResult("my-metric", "gauge", 12);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric:12|g\n"));
        writer.writeQueryResult("my-metric", "g", 13);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric:13|g\n"));

        writer.writeQueryResult("the.answer", "counter", 42);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer:42|c\n"));
        writer.writeQueryResult("the.answer", "c", 43);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer:43|c\n"));

        writer.writeQueryResult("the.answer", "lala", 44);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer:44|c\n"));

    }

    @Test
    public void test_write_counter_metric_dd() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put(StatsDOutputWriter.SETTING_ROOT_PREFIX, "foo.bar");
        // No real connect is done. Config is here to please the postConstruct.
        settings.put(StatsDOutputWriter.SETTING_HOST, "localhost");
        settings.put(StatsDOutputWriter.SETTING_PORT, "8125");
        settings.put(StatsDOutputWriter.SETTINGS_STATSD_TYPE, "dd");
        settings.put(StatsDOutputWriter.SETTINGS_TAGS, "tag1:ok,tag2:woff");

        writer.postConstruct(settings);
        writer.writeQueryResult("my-metric", "gauge", 12);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric:12|g|#tag1:ok,tag2:woff\n"));
        writer.writeQueryResult("my-metric", "g", 13);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric:13|g|#tag1:ok,tag2:woff\n"));

        writer.writeQueryResult("the.answer", "counter", 42);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer:42|c|#tag1:ok,tag2:woff\n"));
        writer.writeQueryResult("the.answer", "c", 43);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer:43|c|#tag1:ok,tag2:woff\n"));

        writer.writeQueryResult("the.answer", "lala", 44);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer:44|c|#tag1:ok,tag2:woff\n"));

    }

    @Test
    public void test_write_counter_metric_dd_without_tags() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put(StatsDOutputWriter.SETTING_ROOT_PREFIX, "foo.bar");
        // No real connect is done. Config is here to please the postConstruct.
        settings.put(StatsDOutputWriter.SETTING_HOST, "localhost");
        settings.put(StatsDOutputWriter.SETTING_PORT, "8125");
        settings.put(StatsDOutputWriter.SETTINGS_STATSD_TYPE, "dd");

        writer.postConstruct(settings);
        writer.writeQueryResult("my-metric", "gauge", 12);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric:12|g\n"));
    }

    @Test
    public void test_write_counter_metric_sysdig() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put(StatsDOutputWriter.SETTING_ROOT_PREFIX, "foo.bar");
        // No real connect is done. Config is here to please the postConstruct.
        settings.put(StatsDOutputWriter.SETTING_HOST, "localhost");
        settings.put(StatsDOutputWriter.SETTING_PORT, "8125");
        settings.put(StatsDOutputWriter.SETTINGS_STATSD_TYPE, "sysdig");
        settings.put(StatsDOutputWriter.SETTINGS_TAGS, "tag1=ok,tag2=woff");

        writer.postConstruct(settings);
        writer.writeQueryResult("my-metric", "gauge", 12);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric#tag1=ok,tag2=woff:12|g\n"));
        writer.writeQueryResult("my-metric", "g", 13);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.my-metric#tag1=ok,tag2=woff:13|g\n"));

        writer.writeQueryResult("the.answer", "counter", 42);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer#tag1=ok,tag2=woff:42|c\n"));
        writer.writeQueryResult("the.answer", "c", 43);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer#tag1=ok,tag2=woff:43|c\n"));

        writer.writeQueryResult("the.answer", "lala", 44);
        Assert.assertThat(writer.receivedStat, equalTo("foo.bar.the.answer#tag1=ok,tag2=woff:44|c\n"));

    }

    @Test
    public void test_skip_counter_with_NaN_value() throws IOException {
        Map<String, String> settings = new HashMap<>();
        settings.put(StatsDOutputWriter.SETTING_ROOT_PREFIX, "foo.bar");
        // No real connect is done. Config is here to please the postConstruct.
        settings.put(StatsDOutputWriter.SETTING_HOST, "localhost");
        settings.put(StatsDOutputWriter.SETTING_PORT, "8125");

        writer.postConstruct(settings);
        writer.writeQueryResult("my-metric", "gauge", "NaN");
        Assert.assertNull(writer.receivedStat);

        writer.writeQueryResult("my-metric", "gauge", "INF");
        Assert.assertNull(writer.receivedStat);
    }

    /**
     * https://github.com/jmxtrans/jmxtrans-agent/issues/98
     */
    @Test
    public void test_BufferOverflowException() throws IOException {
        StatsDOutputWriter writer = new StatsDOutputWriter();
        Map<String, String> config = new HashMap<>();
        config.put(StatsDOutputWriter.SETTING_HOST, "host-does-not-exist.local");
        config.put(StatsDOutputWriter.SETTING_PORT, "8125");
        config.put(StatsDOutputWriter.SETTING_ROOT_PREFIX, "");

        writer.postConstruct(config);

        for (int i = 0; i < 100; i++) {
            writer.writeQueryResult("the.answer", "counter", i);
        }
    }
    public class StatsDOutputWriterMock extends StatsDOutputWriter {
        public String receivedStat;

        @Override
        protected synchronized boolean doSend(String stat) {
            receivedStat = stat;
            return true;
        }
    }


}

