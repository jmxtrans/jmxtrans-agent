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
        settings.put(StatsDOutputWriter.SETTING_HOST, "statsd.example.com");
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

    public class StatsDOutputWriterMock extends StatsDOutputWriter {
        public String receivedStat;

        @Override
        protected synchronized boolean doSend(String stat) {
            receivedStat = stat;
            return true;
        }
    }

}

