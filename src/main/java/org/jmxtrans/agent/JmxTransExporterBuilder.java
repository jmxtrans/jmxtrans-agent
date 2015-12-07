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

import org.jmxtrans.agent.util.PropertyPlaceholderResolver;
import org.jmxtrans.agent.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * XML configuration parser.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransExporterBuilder {

    private static final Pattern ATTRIBUTE_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");
    private Logger logger = Logger.getLogger(getClass().getName());
    private PropertyPlaceholderResolver placeholderResolver = new PropertyPlaceholderResolver();

    public JmxTransExporter build(String configurationFilePath) throws Exception {
        if (configurationFilePath == null) {
            throw new NullPointerException("configurationFilePath cannot be null");
        }
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document document;
        if (configurationFilePath.toLowerCase().startsWith("classpath:")) {
            String classpathResourcePath = configurationFilePath.substring("classpath:".length());
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResourcePath);
            document = dBuilder.parse(in);
        } else if (configurationFilePath.toLowerCase().startsWith("file://") ||
                configurationFilePath.toLowerCase().startsWith("http://") ||
                configurationFilePath.toLowerCase().startsWith("https://")
                ) {
            URL url = new URL(configurationFilePath);
            document = dBuilder.parse(url.openStream());
        } else {
            File xmlFile = new File(configurationFilePath);
            if (!xmlFile.exists()) {
                throw new IllegalArgumentException("Configuration file '" + xmlFile.getAbsolutePath() + "' not found");
            }
            document = dBuilder.parse(xmlFile);
        }

        Element rootElement = document.getDocumentElement();

        JmxTransExporter jmxTransExporter = new JmxTransExporter();

        NodeList collectIntervalNodeList = rootElement.getElementsByTagName("collectIntervalInSeconds");
        switch (collectIntervalNodeList.getLength()) {
            case 0:
                // nothing to do, use default value
                break;
            case 1:
                Element collectIntervalElement = (Element) collectIntervalNodeList.item(0);
                String collectIntervalString = placeholderResolver.resolveString(collectIntervalElement.getTextContent());
                try {
                    jmxTransExporter.withCollectInterval(Integer.parseInt(collectIntervalString), TimeUnit.SECONDS);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Invalid <collectIntervalInSeconds> value '" + collectIntervalString + "', integer expected", e);
                }
                break;
            default:
                logger.warning("More than 1 <collectIntervalInSeconds> element found (" + collectIntervalNodeList.getLength() + "), use latest");
                Element lastCollectIntervalElement = (Element) collectIntervalNodeList.item(collectIntervalNodeList.getLength() - 1);
                String lastCollectIntervalString = placeholderResolver.resolveString(lastCollectIntervalElement.getTextContent());
                try {
                    jmxTransExporter.withCollectInterval(Integer.parseInt(lastCollectIntervalString), TimeUnit.SECONDS);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Invalid <collectIntervalInSeconds> value '" + lastCollectIntervalString + "', integer expected", e);
                }
                break;

        }

        buildResultNameStrategy(rootElement, jmxTransExporter);
        buildInvocations(rootElement, jmxTransExporter);
        buildQueries(rootElement, jmxTransExporter);

        buildOutputWriters(rootElement, jmxTransExporter);

        return jmxTransExporter;
    }

    private void buildQueries(Element rootElement, JmxTransExporter jmxTransExporter) {
        NodeList queries = rootElement.getElementsByTagName("query");
        for (int i = 0; i < queries.getLength(); i++) {
            Element queryElement = (Element) queries.item(i);
            String objectName = queryElement.getAttribute("objectName");
            List<String> attributes = getAttributes(queryElement, objectName);
            String key = queryElement.hasAttribute("key") ? queryElement.getAttribute("key") : null;
            String resultAlias = queryElement.getAttribute("resultAlias");
            String type = queryElement.getAttribute("type");
            Integer position;
            try {
                position = queryElement.hasAttribute("position") ? Integer.parseInt(queryElement.getAttribute("position")) : null;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid 'position' attribute for query objectName=" + objectName +
                        ", attributes=" + attributes + ", resultAlias=" + resultAlias);

            }

            jmxTransExporter.withQuery(objectName, attributes, key, position, type, resultAlias);
        }
    }

    private List<String> getAttributes(Element queryElement, String objectName) {
        String attribute = queryElement.getAttribute("attribute");
        String attributes = queryElement.getAttribute("attributes");
        validateOnlyAttributeOrAttributesSpecified(attribute, attributes, objectName);
        if (!attribute.isEmpty()) {
            return Collections.singletonList(attribute);
        } else {
           String[] splitAttributes = ATTRIBUTE_SPLIT_PATTERN.split(attributes);
           return Arrays.asList(splitAttributes);
        }
    }


    private void validateOnlyAttributeOrAttributesSpecified(String attribute, String attributes, String objectName) {
        if (!attribute.isEmpty() && !attributes.isEmpty()) {
            throw new IllegalArgumentException("Only one of 'attribute' and 'attributes' is supported for a query - not both - objectName: " + objectName);
        }
    }

    private void buildInvocations(Element rootElement, JmxTransExporter jmxTransExporter) {
        NodeList invocations = rootElement.getElementsByTagName("invocation");
        for (int i = 0; i < invocations.getLength(); i++) {
            Element invocationElement = (Element) invocations.item(i);
            String objectName = invocationElement.getAttribute("objectName");
            String operation = invocationElement.getAttribute("operation");
            String resultAlias = invocationElement.getAttribute("resultAlias");

            jmxTransExporter.withInvocation(objectName, operation, resultAlias);
        }
    }

    private void buildResultNameStrategy(Element rootElement, JmxTransExporter jmxTransExporter) {
        NodeList resultNameStrategyNodeList = rootElement.getElementsByTagName("resultNameStrategy");

        ResultNameStrategy resultNameStrategy;
        switch (resultNameStrategyNodeList.getLength()) {
            case 0:
                // nothing to do, use default value
                resultNameStrategy = new ResultNameStrategyImpl();
                break;
            case 1:
                Element resultNameStrategyElement = (Element) resultNameStrategyNodeList.item(0);
                String outputWriterClass = resultNameStrategyElement.getAttribute("class");
                if (outputWriterClass.isEmpty())
                    throw new IllegalArgumentException("<resultNameStrategy> element must contain a 'class' attribute");

                try {
                    resultNameStrategy = (ResultNameStrategy) Class.forName(outputWriterClass).newInstance();
                    Map<String, String> settings = new HashMap<String, String>();
                    NodeList settingsNodeList = resultNameStrategyElement.getElementsByTagName("*");
                    for (int j = 0; j < settingsNodeList.getLength(); j++) {
                        Element settingElement = (Element) settingsNodeList.item(j);
                        settings.put(settingElement.getNodeName(), placeholderResolver.resolveString(settingElement.getTextContent()));
                    }
                    resultNameStrategy.postConstruct(settings);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Exception instantiating " + outputWriterClass, e);
                }

                break;
            default:
                throw new IllegalStateException("More than 1 <resultNameStrategy> element found (" + resultNameStrategyNodeList.getLength() + ")");
        }
        jmxTransExporter.resultNameStrategy = resultNameStrategy;
    }

    private void buildOutputWriters(Element rootElement, JmxTransExporter jmxTransExporter) {
        NodeList outputWriterNodeList = rootElement.getElementsByTagName("outputWriter");
        List<OutputWriter> outputWriters = new ArrayList<OutputWriter>();

        for (int i = 0; i < outputWriterNodeList.getLength(); i++) {
            Element outputWriterElement = (Element) outputWriterNodeList.item(i);
            String outputWriterClass = outputWriterElement.getAttribute("class");
            if (outputWriterClass.isEmpty()) {
                throw new IllegalArgumentException("<outputWriter> element must contain a 'class' attribute");
            }
            OutputWriter outputWriter;
            try {
                outputWriter = (OutputWriter) Class.forName(outputWriterClass).newInstance();
                Map<String, String> settings = new HashMap<String, String>();
                NodeList settingsNodeList = outputWriterElement.getElementsByTagName("*");
                for (int j = 0; j < settingsNodeList.getLength(); j++) {
                    Element settingElement = (Element) settingsNodeList.item(j);
                    settings.put(settingElement.getNodeName(), placeholderResolver.resolveString(settingElement.getTextContent()));
                }
                outputWriter = new OutputWriterCircuitBreakerDecorator(outputWriter);
                outputWriter.postConstruct(settings);
                outputWriters.add(outputWriter);
            } catch (Exception e) {
                throw new IllegalArgumentException("Exception instantiating " + outputWriterClass, e);
            }

        }

        switch (outputWriters.size()) {
            case 0:
                logger.warning("No outputwriter defined.");
                break;
            case 1:
                jmxTransExporter.withOutputWriter(outputWriters.get(0));
                break;
            default:
                jmxTransExporter.withOutputWriter(new OutputWritersChain(outputWriters));
        }
    }
}
