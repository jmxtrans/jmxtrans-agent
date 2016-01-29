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
package org.jmxtrans.agent.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads properties from a URL or a file.
 * 
 * Uses the same logic to locate a file as {@link org.jmxtrans.agent.JmxTransConfigurationDocumentLoader}.
 * 
 * @author Kristoffer Erlandsson
 */
public class UrlOrFilePropertiesLoader implements PropertiesLoader {

    private final String propertiesPath;

    public UrlOrFilePropertiesLoader(String propertiesPath) {
        this.propertiesPath = propertiesPath;
    }

    @Override
    public Map<String, String> loadProperties() {
        try {
            return convertToMap(tryLoadProperties());
        } catch (Exception e) {
            throw new FailedToLoadPropertiesException("Failed to load properties file '" + propertiesPath + "'", e);
        }
    }

    private Map<String, String> convertToMap(Properties properties) {
        Map<String, String> m = new HashMap<>();
        for (Object key : properties.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Properties key is not string");
            }
            String stringKey = (String) key;
            m.put(stringKey, properties.getProperty(stringKey));
        }
        return m;
    }

    private Properties tryLoadProperties() throws Exception {
        Properties properties = new Properties();
        if (isClassPathSpec()) {
            loadFromClasspath(properties);
        } else if (isUrl()) {
            loadFromUrl(properties);
        } else {
            loadFromFile(properties);
        }
        return properties;
    }

    private boolean isClassPathSpec() {
        return propertiesPath.toLowerCase().startsWith("classpath:");
    }

    private boolean isUrl() {
        return propertiesPath.toLowerCase().startsWith("file://") ||
                propertiesPath.toLowerCase().startsWith("http://") ||
                propertiesPath.toLowerCase().startsWith("https://");
    }

    private void loadFromFile(Properties properties) throws IOException, FileNotFoundException {
        File file = new File(propertiesPath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Properties file '" + file.getAbsolutePath() + "' not found");
        }
        try (InputStream stream = new FileInputStream(file)) {
            properties.load(stream);
        }
    }

    private void loadFromUrl(Properties properties) throws MalformedURLException, IOException {
        URL url = new URL(propertiesPath);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.connect();
        try (InputStream stream = connection.getInputStream()) {
            properties.load(stream);
        }
    }

    private void loadFromClasspath(Properties properties) throws IOException {
        String classpathResourcePath = propertiesPath.substring("classpath:".length());
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathResourcePath)) {
            properties.load(in);
        }
    }

    @SuppressWarnings("serial")
    public static class FailedToLoadPropertiesException extends RuntimeException {

        public FailedToLoadPropertiesException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    @Override
    public String toString() {
        return "UrlOrFilePropertiesLoader, path='" + propertiesPath + "'";
    }

}
