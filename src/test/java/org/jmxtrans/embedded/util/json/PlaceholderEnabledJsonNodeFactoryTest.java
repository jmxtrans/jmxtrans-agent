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
package org.jmxtrans.embedded.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class PlaceholderEnabledJsonNodeFactoryTest {

    @Test
    public void testWithPlaceholders() throws Exception {
        System.setProperty("graphite.host", "graphite.www.private.mycompany.com");
        System.setProperty("server.name", "tomcat1");
        try {
            String configurationUrl = "org/jmxtrans/embedded/util/json/jmxtrans-placeholder-test.json";
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(configurationUrl);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setNodeFactory(new PlaceholderEnabledJsonNodeFactory());
            JsonNode rootNode = objectMapper.readValue(in, JsonNode.class);
            JsonNode outputWritersNode = rootNode.path("outputWriters");
            JsonNode outputWriterNode = outputWritersNode.get(1);
            assertThat(outputWriterNode.path("@class").asText(), is("org.jmxtrans.embedded.output.GraphiteWriter"));
            JsonNode settingsNode = outputWriterNode.path("settings");
            assertThat(settingsNode.path("host").asText(), is("graphite.www.private.mycompany.com"));
            assertThat(settingsNode.path("port").asInt(), is(2003));
            assertThat(settingsNode.path("namePrefix").asText(), is("servers.tomcat1."));

        } finally {
            System.getProperties().remove("graphite.host");
            System.getProperties().remove("server.name");
        }
    }
}
