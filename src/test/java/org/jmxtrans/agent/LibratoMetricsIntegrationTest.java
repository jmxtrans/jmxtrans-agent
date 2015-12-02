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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class LibratoMetricsIntegrationTest {

    LibratoWriter libratoWriter;

    @Before
    public void before() throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("my-librato-config.properties");
        Assert.assertNotNull("File src/test/resources/my-librato-config.properties is missing. See librato-config.template.properties for details", in);
        Properties config = new Properties();
        config.load(in);

        libratoWriter = new LibratoWriter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put(LibratoWriter.SETTING_USERNAME,config.getProperty("LIBRATO_USER"));
        settings.put(LibratoWriter.SETTING_TOKEN, config.getProperty("LIBRATO_TOKEN"));

        libratoWriter.postConstruct(settings);
    }


    @Category(IntegrationTest.class)
    @Test
    public void testWithOneCounter() throws Exception {
        libratoWriter.writeQueryResult("test-with-one-counter.singleresult", "counter", 10);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

    @Category(IntegrationTest.class)
    @Test
    public void testWithOneGauge() throws Exception {
        libratoWriter.writeQueryResult("test-with-one-gauge.singleresult", "gauge", 10);

        Assert.assertThat(libratoWriter.getExceptionCounter(), equalTo(0));
    }

}
