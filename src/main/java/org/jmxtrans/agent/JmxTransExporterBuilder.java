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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * XML configuration parser.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransExporterBuilder {

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

        buildQueries(rootElement, jmxTransExporter);

        buildOutputWriters(rootElement, jmxTransExporter);

        return jmxTransExporter;
    }

    private void buildQueries(Element rootElement, JmxTransExporter jmxTransExporter) {
        NodeList queries = rootElement.getElementsByTagName("query");
        for (int i = 0; i < queries.getLength(); i++) {
            Element queryElement = (Element) queries.item(i);
            String objectName = queryElement.getAttribute("objectName");
            String attribute = queryElement.getAttribute("attribute");
            String key = queryElement.hasAttribute("key") ? queryElement.getAttribute("key") : null;
            String resultAlias = queryElement.getAttribute("resultAlias");

            jmxTransExporter.withQuery(objectName, attribute, key, resultAlias);
        }
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
