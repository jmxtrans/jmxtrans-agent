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

import org.jmxtrans.embedded.QueryResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class LibratoMetricsIntegrationTest {

    LibratoWriter libratoWriter;

    @Before
    public void before() {
        libratoWriter = new LibratoWriter();
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put(AbstractOutputWriter.SETTING_USERNAME, "<<YOUR_USERNAME>>");
        settings.put(AbstractOutputWriter.SETTING_TOKEN, "<<YOUR_TOKEN>>");

        libratoWriter.setSettings(settings);
        libratoWriter.start();
    }

    @After
    public void after() throws Exception {
        libratoWriter.stop();
    }

    @Test
    public void testWithOneCounter() throws Exception {
        List<QueryResult> results = Arrays.asList(
                new QueryResult("test-with-one-counter.singleresult", "counter", 10, System.currentTimeMillis()));
        libratoWriter.write(results);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

    @Test
    public void testWithOneGauge() throws Exception {
        List<QueryResult> results = Arrays.asList(
                new QueryResult("test-with-one-gauge.singleresult", "gauge", 10, System.currentTimeMillis()));
        libratoWriter.write(results);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

    @Test
    public void testWithTwoCounters() throws Exception {

        List<QueryResult> results = Arrays.asList(
                new QueryResult("test-with-two-counters.first", "counter", 10, System.currentTimeMillis()),
                new QueryResult("test-with-two-counters.second", "counter", 20, System.currentTimeMillis()));

        libratoWriter.write(results);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

    @Test
    public void testWithTwoGauges() throws Exception {

        List<QueryResult> results = Arrays.asList(
                new QueryResult("test-with-two-gauges.first", "gauge", 10, System.currentTimeMillis()),
                new QueryResult("test-with-two-gauges.second", "gauge", 20, System.currentTimeMillis()));

        libratoWriter.write(results);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

    @Test
    public void testWithThreeCounters() throws Exception {

        List<QueryResult> results = Arrays.asList(
                new QueryResult("test-with-three-counters.first", "counter", 10, System.currentTimeMillis() - 1000),
                new QueryResult("test-with-three-counters.first", "counter", 20, System.currentTimeMillis()),
                new QueryResult("test-with-three-counters.second", "counter", 30, System.currentTimeMillis()));

        libratoWriter.write(results);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

}
