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
package org.jmxexporter;

import org.jmxexporter.config.ConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class JmxExporterFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JmxExporter jmxExporter;

    private String configurationUrl;

    @PostConstruct
    public JmxExporter getObject() throws Exception {
        logger.info("Load JmxExporter('{}')", configurationUrl);
        jmxExporter = new ConfigurationParser().newJmxExporter(configurationUrl);
        logger.info("Start JmxExporter('{}')", configurationUrl);
        jmxExporter.start();
        return jmxExporter;
    }

    public String getConfigurationUrl() {
        return configurationUrl;
    }

    public void setConfigurationUrl(String configurationUrl) {
        this.configurationUrl = configurationUrl;
    }
}
