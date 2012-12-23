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
import org.jmxexporter.util.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class JmxExporterServer {

    private final Logger logger = LoggerFactory.getLogger(getClass());



    private JmxExporter configuration;

    private String configurationUrl;

    public JmxExporterServer() {
    }

    @PostConstruct
    public void start() {
        try {
            InputStream in;
            if (configurationUrl.startsWith("classpath:")) {
                String path = this.configurationUrl.substring("classpath:".length());
                in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            } else {
                in = new URL(configurationUrl).openStream();
            }
            configuration = new ConfigurationParser().parseConfiguration(in);
        } catch (Exception e) {
            throw new JmxExporterException("Exception loading configuration'" + configurationUrl + "'", e);
        }
    }

    public String getConfigurationUrl() {
        return configurationUrl;
    }

    public void setConfigurationUrl(String configurationUrl) {
        this.configurationUrl = configurationUrl;
    }
}
