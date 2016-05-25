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

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.HashMap;
import java.util.Map;

import org.jmxtrans.agent.testutils.FixedTimeClock;
import org.jmxtrans.agent.util.time.Clock;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * @author Kristoffer Erlandsson
 */
public class InfluxDbOutputWriterTest {

    private final static Clock FAKE_CLOCK = new FixedTimeClock(1234l);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void simpleRequest() throws Exception {
        Map<String, String> s = new HashMap<>();
        s.put("url", "http://localhost:" + wireMockRule.port());
        s.put("database", "test-db");
        stubFor(post(urlPathEqualTo("/write")).willReturn(aResponse().withStatus(200)));
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter(FAKE_CLOCK);
        writer.postConstruct(s);
        writer.writeQueryResult("foo", null, 1);
        writer.postCollect();
        verify(postRequestedFor(urlPathEqualTo("/write"))
                .withQueryParam("db", equalTo("test-db"))
                .withQueryParam("precision", equalTo("ms"))
                .withRequestBody(equalTo("foo value=1i 1234")));
    }

    @Test
    public void allConfigParameters() throws Exception {
        Map<String, String> s = new HashMap<>();
        s.put("url", "http://localhost:" + wireMockRule.port());
        s.put("database", "test-db");
        s.put("retentionPolicy", "policy");
        s.put("user", "admin");
        s.put("password", "shadow");
        s.put("tags", "t1=v1,t2=v2");
        s.put("connectTimeoutMillis", "1000");
        s.put("readTimeoutMillis", "5000");
        stubFor(post(urlPathEqualTo("/write")).willReturn(aResponse().withStatus(200)));
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter(FAKE_CLOCK);
        writer.postConstruct(s);
        writer.writeQueryResult("foo", null, 1);
        writer.postCollect();
        verify(postRequestedFor(urlPathEqualTo("/write"))
                .withQueryParam("db", equalTo("test-db"))
                .withQueryParam("precision", equalTo("ms"))
                .withQueryParam("rp", equalTo("policy"))
                .withQueryParam("u", equalTo("admin"))
                .withQueryParam("p", equalTo("shadow"))
                .withRequestBody(equalTo("foo,t1=v1,t2=v2 value=1i 1234")));
    }

    @Test
    public void tagsInMetricName() throws Exception {
        Map<String, String> s = new HashMap<>();
        s.put("url", "http://localhost:" + wireMockRule.port());
        s.put("database", "test-db");
        stubFor(post(urlPathEqualTo("/write")).willReturn(aResponse().withStatus(200)));
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter(FAKE_CLOCK);
        writer.postConstruct(s);
        writer.writeQueryResult("foo,tag=tagValue", null, 1);
        writer.postCollect();
        verify(postRequestedFor(urlPathEqualTo("/write"))
                .withQueryParam("db", equalTo("test-db"))
                .withQueryParam("precision", equalTo("ms"))
                .withRequestBody(equalTo("foo,tag=tagValue value=1i 1234")));
    }

    @Test
    public void manyMetrics() throws Exception {
        Map<String, String> s = new HashMap<>();
        s.put("url", "http://localhost:" + wireMockRule.port());
        s.put("database", "test-db");
        stubFor(post(urlPathEqualTo("/write")).willReturn(aResponse().withStatus(200)));
        InfluxDbOutputWriter writer = new InfluxDbOutputWriter(FAKE_CLOCK);
        writer.postConstruct(s);
        writer.writeQueryResult("foo", null, 1);
        writer.writeQueryResult("foo2", null, 2.0);
        writer.postCollect();
        verify(postRequestedFor(urlPathEqualTo("/write"))
                .withQueryParam("db", equalTo("test-db"))
                .withQueryParam("precision", equalTo("ms"))
                .withRequestBody(equalTo("foo value=1i 1234\nfoo2 value=2.0 1234")));
    }

}
