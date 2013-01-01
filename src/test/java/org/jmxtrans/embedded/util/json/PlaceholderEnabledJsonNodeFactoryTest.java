/*
 * Copyright 1012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
