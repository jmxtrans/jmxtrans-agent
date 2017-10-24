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
package org.jmxtrans.agent;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.jmxtrans.agent.graphite.GraphiteMetricMessageBuilder;
import org.junit.Test;

/**
 * @author Kristoffer Erlandsson
 */
public class GraphiteMetricMessageBuilderTest {

    @Test
    public void configuredPrefix() throws Exception {
        GraphiteMetricMessageBuilder builder = new GraphiteMetricMessageBuilder("foo.");
        String msg = builder.buildMessage("bar", 2, 11);
        assertThat(msg, equalTo("foo.bar 2 11"));
    }

    @Test
    public void defaultPrefix() throws Exception {
        GraphiteMetricMessageBuilder builder = new GraphiteMetricMessageBuilder(null);
        String msg = builder.buildMessage("bar", 2, 11);
        assertThat(msg, startsWith("servers."));
        assertThat(msg, endsWith(" 2 11"));
    }
    
    @Test
    public void trueIsConvertedToOne() {
        GraphiteMetricMessageBuilder builder = new GraphiteMetricMessageBuilder("foo.");
        String msg = builder.buildMessage("bar", true, 11);
        assertThat(msg, equalTo("foo.bar 1 11"));
    }

    @Test
    public void falseIsConvertedToZero() {
        GraphiteMetricMessageBuilder builder = new GraphiteMetricMessageBuilder("foo.");
        String msg = builder.buildMessage("bar", false, 11);
        assertThat(msg, equalTo("foo.bar 0 11"));
    }
}
