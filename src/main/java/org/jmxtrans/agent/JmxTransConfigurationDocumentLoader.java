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

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class JmxTransConfigurationDocumentLoader implements ConfigurationDocumentLoader {

    private final String configurationFilePath;


    /**
     * Creates a loader that will load a document from the specified path.
     */
    public JmxTransConfigurationDocumentLoader(String configurationFilePath) {
        if (configurationFilePath == null) {
            throw new NullPointerException("configurationFilePath cannot be null");
        }
        this.configurationFilePath = configurationFilePath;
    }


    @Override
    public Document loadConfiguration() throws Exception {
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
        return document;
    }

    @Override
    public String toString() {
        return configurationFilePath;
    }
}
