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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class ConfigReloadWatcherTest {

    String testXml = "<jmxtrans-agent>\r\n" + 
            "    <outputWriter class=\"org.jmxtrans.agent.ConsoleOutputWriter\" />\r\n" + 
            "    <reloadConfigurationCheckIntervalInSeconds>0</reloadConfigurationCheckIntervalInSeconds>\r\n" + 
            "</jmxtrans-agent>";
            
    private FakeConfigurationChangedListener listener;

    private FakeConfigLoader configLoader;

    private JmxTransExporterConfiguration initialConfiguration;

    private ConfigReloadWatcher watcher;
    
    
    @Before
    public void setUp() throws Exception {
       listener = new FakeConfigurationChangedListener();
       configLoader = new FakeConfigLoader(testXml);
       initialConfiguration = new JmxTransExporterConfiguration(configLoader.loadConfiguration());
       initialConfiguration.withConfigReloadInterval(0);
       watcher = new ConfigReloadWatcher(listener, initialConfiguration, configLoader); 
    }

    @After
    public void stopWatcher() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Test
    public void listenerNotNotifiedWhenConfigurationDoesNotChange() throws Exception {
       watcher.start();
       Thread.sleep(100);
       assertThat(listener.notifiedWith, nullValue());
    }

    @Test
    public void listenerNotified() throws Exception {
       watcher.start();
       configLoader.setXml(testXml + "<!-- -->");
       assertEventuallyListenerNotifiedWith(listener, notNullValue(JmxTransExporterConfiguration.class));
    }
    
    @Test
    public void errorDuringConfigurationLoad() throws Exception {
       watcher.start();
       configLoader.setExceptionToThrow(new Exception());
       configLoader.setXml(testXml + "<!-- -->");
       Thread.sleep(50);
       assertThat(listener.notifiedWith, nullValue()); // No notificactions when config cannot be loaded
       configLoader.setExceptionToThrow(null);
       assertEventuallyListenerNotifiedWith(listener, notNullValue(JmxTransExporterConfiguration.class));
    }
    
    public void assertEventuallyListenerNotifiedWith(FakeConfigurationChangedListener listener, Matcher<JmxTransExporterConfiguration> matcher)
            throws Exception {
        for (int i = 0; i < 100; i++) {
            if (matcher.matches(listener.notifiedWith)) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(listener.notifiedWith, matcher);
    }
    
    private static class FakeConfigurationChangedListener implements ConfigurationChangedListener {

        private volatile JmxTransExporterConfiguration notifiedWith;

        @Override
        public void configurationChanged(JmxTransExporterConfiguration configuration) {
            this.notifiedWith = configuration;
        }
        
    }
    
    private static class FakeConfigLoader implements ConfigurationDocumentLoader {
        
        private volatile String xml;
        private volatile Exception exception;

        public FakeConfigLoader(String xml) {
            this.xml = xml;
        }

        @Override
        public Document loadConfiguration() throws Exception {
            if (exception != null) {
                throw exception;
            }
            InputStream is = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
        
        public void setXml(String xml) {
            this.xml = xml;
        }
        
        public void setExceptionToThrow(Exception e) {
            this.exception = e;
        }

    }
}
