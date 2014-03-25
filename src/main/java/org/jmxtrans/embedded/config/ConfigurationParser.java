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
import java.util.*;

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
    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull String configurationUrl) throws EmbeddedJmxTransException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();
        mergeEmbeddedJmxTransConfiguration(configurationUrl, embeddedJmxTrans);
        return embeddedJmxTrans;
    }

    protected void mergeEmbeddedJmxTransConfiguration(@Nonnull String configurationUrl, @Nonnull EmbeddedJmxTrans embeddedJmxTrans) throws EmbeddedJmxTransException {
        try {
            if (configurationUrl.startsWith("classpath:")) {
                logger.debug("mergeEmbeddedJmxTransConfiguration({})", configurationUrl);
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

    @Nonnull
    public EmbeddedJmxTrans newEmbeddedJmxTrans(@Nonnull InputStream configuration) throws IOException {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();
        mergeEmbeddedJmxTransConfiguration(configuration, embeddedJmxTrans);
        return embeddedJmxTrans;
    }

    public void mergeEmbeddedJmxTransConfiguration(@Nonnull InputStream configuration, EmbeddedJmxTrans embeddedJmxTrans) throws IOException {
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
        logger.debug("mergeEmbeddedJmxTransConfiguration({})", configurationUrl);
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
            logger.trace("Add {}", query);
        }

        List<OutputWriter> outputWriters = parseOutputWritersNode(configurationRootNode);
        embeddedJmxTrans.getOutputWriters().addAll(outputWriters);
        logger.trace("Add global output writers: {}", outputWriters);

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
                    JsonNode deprecatedEnabledNode = outputWriterNode.path("enabled");
                    if (!deprecatedEnabledNode.isMissingNode()) {
                        logger.warn("OutputWriter {}, deprecated usage of attribute 'enabled', settings{ \"enabled\":... } should be used instead");
                        outputWriter.setEnabled(deprecatedEnabledNode.asBoolean());
                    }
                    JsonNode settingsNode = outputWriterNode.path("settings");
                    if (settingsNode.isMissingNode()) {
                    } else if (settingsNode.isObject()) {
                        ObjectMapper mapper = new ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> settings = mapper.treeToValue(settingsNode, Map.class);
                        outputWriter.setSettings(settings);
                        if (settings.containsKey("enabled")) {
                            outputWriter.setEnabled(Boolean.valueOf(String.valueOf(settings.get("enabled"))));
                        }
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
            JsonNode typeNode = attributeNode.path("type");
            String type = typeNode.isMissingNode() ? null : typeNode.asText();
            if (keys == null) {
                query.addAttribute(new QueryAttribute(name, type, resultAlias));
            } else {
                query.addAttribute(new QueryAttribute(name, type, resultAlias, keys));
            }
        } else {
            logger.warn("Ignore invalid node {}", attributeNode);
        }
    }
}
