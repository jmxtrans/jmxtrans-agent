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
package org.jmxtrans.embedded.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmxtrans.embedded.*;
import org.jmxtrans.embedded.output.OutputWriter;
import org.jmxtrans.embedded.util.Preconditions;
import org.jmxtrans.embedded.util.json.PlaceholderEnabledJsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON Configuration parser to build {@link org.jmxtrans.embedded.EmbeddedJmxTrans}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ConfigurationParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper;

    {
        mapper = new ObjectMapper();
        mapper.setNodeFactory(new PlaceholderEnabledJsonNodeFactory());
    }

    public EmbeddedJmxTrans newEmbeddedJmxTrans(String... configurationUrls) throws EmbeddedJmxTransException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();

        for (String configurationUrl : configurationUrls) {
            mergeEmbeddedJmxTransConfiguration(configurationUrl, embeddedJmxTrans);
        }
        return embeddedJmxTrans;
    }

    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull List<String> configurationUrls) throws EmbeddedJmxTransException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();

        for (String configurationUrl : configurationUrls) {
            mergeEmbeddedJmxTransConfiguration(configurationUrl, embeddedJmxTrans);
        }
        return embeddedJmxTrans;
    }

    /**
     * @param configurationUrl JSON configuration file URL ("http://...", "classpath:com/mycompany...", ...)
     */
    @Nonnull
    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull String configurationUrl)throws EmbeddedJmxTransException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();
        mergeEmbeddedJmxTransConfiguration(configurationUrl, embeddedJmxTrans);
        return embeddedJmxTrans;
    }

    protected void mergeEmbeddedJmxTransConfiguration(@Nonnull String configurationUrl, @Nonnull EmbeddedJmxTrans embeddedJmxTrans) throws EmbeddedJmxTransException {
        try {
            if (configurationUrl.startsWith("classpath:")) {
                String path = configurationUrl.substring("classpath:".length());
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
                Preconditions.checkNotNull(in, "No file found for '" + configurationUrl + "'");
                mergeEmbeddedJmxTransConfiguration(in, embeddedJmxTrans);
            } else {
                mergeEmbeddedJmxTransConfiguration(new URL(configurationUrl), embeddedJmxTrans);
            }
        } catch (JsonProcessingException e) {
            throw new EmbeddedJmxTransException("Exception loading configuration'" + configurationUrl + "': " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmbeddedJmxTransException("Exception loading configuration'" + configurationUrl + "'", e);
        }
    }

    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull InputStream configuration) throws IOException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();
        mergeEmbeddedJmxTransConfiguration(configuration, embeddedJmxTrans);
        return embeddedJmxTrans;
    }

    protected void mergeEmbeddedJmxTransConfiguration(@Nonnull InputStream configuration, EmbeddedJmxTrans embeddedJmxTrans) throws IOException {
        JsonNode configurationRootNode = mapper.readValue(configuration, JsonNode.class);
        mergeEmbeddedJmxTransConfiguration(configurationRootNode, embeddedJmxTrans);
    }

    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull URL configurationUrl) throws IOException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();
        mergeEmbeddedJmxTransConfiguration(configurationUrl, embeddedJmxTrans);
        return embeddedJmxTrans;
    }

    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull JsonNode configurationRootNode) {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();
        mergeEmbeddedJmxTransConfiguration(configurationRootNode, embeddedJmxTrans);
        return embeddedJmxTrans;
    }

    protected void mergeEmbeddedJmxTransConfiguration(@Nonnull URL configurationUrl, EmbeddedJmxTrans embeddedJmxTrans) throws IOException {
        JsonNode configurationRootNode = mapper.readValue(configurationUrl, JsonNode.class);
        mergeEmbeddedJmxTransConfiguration(configurationRootNode, embeddedJmxTrans);
    }

    private void mergeEmbeddedJmxTransConfiguration(@Nonnull JsonNode configurationRootNode, @Nonnull EmbeddedJmxTrans embeddedJmxTrans) {
        for (JsonNode queryNode : configurationRootNode.path("queries")) {

            String objectName = queryNode.path("objectName").asText();
            Query query = new Query(objectName);
            embeddedJmxTrans.addQuery(query);
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
        embeddedJmxTrans.getOutputWriters().addAll(outputWriters);

        JsonNode queryIntervalInSecondsNode = configurationRootNode.path("queryIntervalInSeconds");
        if (!queryIntervalInSecondsNode.isMissingNode()) {
            embeddedJmxTrans.setQueryIntervalInSeconds(queryIntervalInSecondsNode.asInt());
        }

        JsonNode exportBatchSizeNode = configurationRootNode.path("exportBatchSize");
        if (!exportBatchSizeNode.isMissingNode()) {
            embeddedJmxTrans.setExportBatchSize(exportBatchSizeNode.asInt());
        }

        JsonNode numQueryThreadsNode = configurationRootNode.path("numQueryThreads");
        if (!numQueryThreadsNode.isMissingNode()) {
            embeddedJmxTrans.setNumQueryThreads(numQueryThreadsNode.asInt());
        }

        JsonNode exportIntervalInSecondsNode = configurationRootNode.path("exportIntervalInSeconds");
        if (!exportIntervalInSecondsNode.isMissingNode()) {
            embeddedJmxTrans.setExportIntervalInSeconds(exportIntervalInSecondsNode.asInt());
        }

        JsonNode numExportThreadsNode = configurationRootNode.path("numExportThreads");
        if (!numExportThreadsNode.isMissingNode()) {
            embeddedJmxTrans.setNumExportThreads(numExportThreadsNode.asInt());
        }

        logger.info("Loaded {}", embeddedJmxTrans);
    }

    private List<OutputWriter> parseOutputWritersNode(@Nonnull JsonNode outputWritersParentNode) {
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
                        @SuppressWarnings("unchecked")
                        Map<String, Object> settings = mapper.treeToValue(settingsNode, Map.class);
                        outputWriter.setSettings(settings);
                    } else {
                        logger.warn("Ignore invalid node {}", outputWriterNode);
                    }
                    outputWriters.add(outputWriter);
                } catch (Exception e) {
                    throw new EmbeddedJmxTransException("Exception converting settings " + outputWritersNode, e);
                }
            }
        } else {
            logger.warn("Ignore invalid node {}", outputWritersNode);
        }
        return outputWriters;
    }

    protected void parseQueryAttributeNode(@Nonnull Query query, @Nonnull JsonNode attributeNode) {
        if (attributeNode.isMissingNode()) {
        } else if (attributeNode.isValueNode()) {
            query.addAttribute(attributeNode.asText());
        } else if (attributeNode.isObject()) {
            List<String> keys = null;

            JsonNode keysNode = attributeNode.path("keys");
            if (keysNode.isMissingNode()) {
            } else if (keysNode.isArray()) {
                if (keys == null) {
                    keys = new ArrayList<String>();
                }
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
                if (keys == null) {
                    keys = new ArrayList<String>();
                }
                keys.add(keyNode.asText());
            } else {
                logger.warn("Ignore invalid node {}", keyNode);
            }

            String name = attributeNode.path("name").asText();
            JsonNode resultAliasNode = attributeNode.path("resultAlias");
            String resultAlias = resultAliasNode.isMissingNode() ? null : resultAliasNode.asText();
            if (keys == null) {
                query.addAttribute(new QueryAttribute(name, resultAlias));
            } else {
                query.addAttribute(new QueryAttribute(name, resultAlias, keys));
            }
        } else {
            logger.warn("Ignore invalid node {}", attributeNode);
        }
    }
}
