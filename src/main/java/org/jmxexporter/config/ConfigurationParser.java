/*
 * Copyright 2008-2012 Xebia and the original author or authors.
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
package org.jmxexporter.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmxexporter.*;
import org.jmxexporter.output.OutputWriter;
import org.jmxexporter.util.json.PlaceholderEnabledJsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ConfigurationParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ObjectMapper mapper;

    {
        mapper = new ObjectMapper();
        mapper.setNodeFactory(new PlaceholderEnabledJsonNodeFactory());
    }

    /**
     * @param configurationUrl JSON configuration file URL ("http://...", "classpath:com/mycompany...", ...)
     * @return
     */
    public JmxExporter newJmxExporter(String configurationUrl) {
        try {
            if (configurationUrl.startsWith("classpath:")) {
                String path = configurationUrl.substring("classpath:".length());
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
                return new ConfigurationParser().newJmxExporter(in);
            } else {
                return newJmxExporter(new URL(configurationUrl));
            }
        } catch (Exception e) {
            throw new JmxExporterException("Exception loading configuration'" + configurationUrl + "'", e);
        }

    }

    public JmxExporter newJmxExporter(InputStream configuration) throws IOException {
        JsonNode rootNode = mapper.readValue(configuration, JsonNode.class);
        return newJmxExporter(rootNode);
    }

    public JmxExporter newJmxExporter(URL configurationUrl) throws IOException {
        JsonNode rootNode = mapper.readValue(configurationUrl, JsonNode.class);
        return newJmxExporter(rootNode);
    }

    public JmxExporter newJmxExporter(JsonNode configurationRootNode) {

        JmxExporter jmxExporter = new JmxExporter();

        for (JsonNode queryNode : configurationRootNode.path("queries")) {

            String objectName = queryNode.path("objectName").asText();
            Query query = new Query(objectName);
            jmxExporter.addQuery(query);
            JsonNode resultAliasNode = queryNode.path("resultAlias");
            if (resultAliasNode.isMissingNode()) {
            } else if (resultAliasNode.isValueNode()) {
                query.setResultAlias(resultAliasNode.asText());
            } else {
                logger.warn("Ignore invalid node {}", resultAliasNode);
            }

            JsonNode attributesNode = queryNode.path("attributes");
            if (attributesNode.isMissingNode()) {
            } else if (attributesNode.isArray()) {
                Iterator<JsonNode> itAttributeNode = attributesNode.elements();
                while (itAttributeNode.hasNext()) {
                    JsonNode attributeNode = itAttributeNode.next();
                    parseQueryAttributeNode(query, attributeNode);
                }
            } else {
                logger.warn("Ignore invalid node {}", resultAliasNode);
            }

            JsonNode attributeNode = queryNode.path("attribute");
            parseQueryAttributeNode(query, attributeNode);
            List<OutputWriter> outputWriters = parseOutputWritersNode(queryNode);
            query.getOutputWriters().addAll(outputWriters);
        }


        List<OutputWriter> outputWriters = parseOutputWritersNode(configurationRootNode);
        jmxExporter.getOutputWriters().addAll(outputWriters);

        JsonNode queryIntervalInSecondsNode = configurationRootNode.path("queryIntervalInSeconds");
        if (!queryIntervalInSecondsNode.isMissingNode()) {
            jmxExporter.setQueryIntervalInSeconds(queryIntervalInSecondsNode.asInt());
        }

        JsonNode exportBatchSizeNode = configurationRootNode.path("exportBatchSize");
        if (!exportBatchSizeNode.isMissingNode()) {
            jmxExporter.setExportBatchSize(exportBatchSizeNode.asInt());
        }

        JsonNode numQueryThreadsNode = configurationRootNode.path("numQueryThreads");
        if (!numQueryThreadsNode.isMissingNode()) {
            jmxExporter.setNumQueryThreads(numQueryThreadsNode.asInt());
        }

        JsonNode exportIntervalInSecondsNode = configurationRootNode.path("exportIntervalInSeconds");
        if (!exportIntervalInSecondsNode.isMissingNode()) {
            jmxExporter.setExportIntervalInSeconds(exportIntervalInSecondsNode.asInt());
        }


        JsonNode numExportThreadsNode = configurationRootNode.path("numExportThreads");
        if (!numExportThreadsNode.isMissingNode()) {
            jmxExporter.setNumExportThreads(numExportThreadsNode.asInt());
        }

        logger.info("Loaded {}", jmxExporter);
        return jmxExporter;
    }

    private List<OutputWriter> parseOutputWritersNode(JsonNode outputWritersParentNode) {
        JsonNode outputWritersNode = outputWritersParentNode.path("outputWriters");
        List<OutputWriter> outputWriters = new ArrayList<OutputWriter>();
        if (outputWritersNode.isMissingNode()) {
        } else if (outputWritersNode.isArray()) {
            for (JsonNode outputWriterNode : outputWritersNode) {
                try {
                    String className = outputWriterNode.path("@class").asText();
                    OutputWriter outputWriter = (OutputWriter) Class.forName(className).newInstance();
                    JsonNode settingsNode = outputWriterNode.path("settings");
                    if (settingsNode.isMissingNode()) {
                    } else if (settingsNode.isObject()) {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> settings = mapper.treeToValue(settingsNode, Map.class);
                        outputWriter.setSettings(settings);
                    } else {
                        logger.warn("Ignore invalid node {}", outputWriterNode);
                    }
                    outputWriters.add(outputWriter);
                } catch (Exception e) {
                    throw new JmxExporterException("Exception converting settings " + outputWritersNode, e);
                }
            }
        } else {
            logger.warn("Ignore invalid node {}", outputWritersNode);
        }
        return outputWriters;
    }

    protected void parseQueryAttributeNode(Query query, JsonNode attributeNode) {
        if (attributeNode.isMissingNode()) {
        } else if (attributeNode.isValueNode()) {
            query.addAttribute(attributeNode.asText());
        } else if (attributeNode.isObject()) {
            List<String> keys = new ArrayList<String>();

            JsonNode keysNode = attributeNode.path("keys");
            if (keysNode.isMissingNode()) {
            } else if (keysNode.isArray()) {
                Iterator<JsonNode> itAttributeNode = keysNode.elements();
                while (itAttributeNode.hasNext()) {
                    JsonNode keyNode = itAttributeNode.next();
                    if (keyNode.isValueNode()) {
                        keys.add(keyNode.asText());
                    } else {
                        logger.warn("Ignore invalid node {}", keyNode);
                    }
                }
            } else {
                logger.warn("Ignore invalid node {}", keysNode);
            }

            JsonNode keyNode = attributeNode.path("key");
            if (keyNode.isMissingNode()) {
            } else if (keyNode.isValueNode()) {
                keys.add(keyNode.asText());
            } else {
                logger.warn("Ignore invalid node {}", keyNode);
            }

            String name = attributeNode.path("name").asText();
            JsonNode resultAliasNode = attributeNode.path("resultAlias");
            String resultAlias = resultAliasNode.isMissingNode() ? null : resultAliasNode.asText();
            if (keys.isEmpty()) {
                query.addAttribute(new QueryAttribute(name, resultAlias));
            } else {
                query.addAttribute(new QueryCompositeAttribute(name, resultAlias, keys));
            }
        } else {
            logger.warn("Ignore invalid node {}", attributeNode);
        }
    }
}
