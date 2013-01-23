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
package org.jmxtrans.embedded.spring;

import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.config.ConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.*;

/**
 * {@link org.jmxtrans.embedded.EmbeddedJmxTrans} factory for Spring Framework integration.
 * <p/>
 * Default {@linkplain #configurationUrls} :
 * <ul>
 * <li><code>classpath:jmxtrans.json</code>: expected to be provided by the application</li>
 * <li><code>classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json</code>: provided by jmxtrans jar for its internal monitoring</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class EmbeddedJmxTransFactory implements FactoryBean<SpringEmbeddedJmxTrans>, BeanNameAware {

    private final static String DEFAULT_CONFIGURATION_URL = "classpath:jmxtrans.json, classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> configurationUrls;

    private SpringEmbeddedJmxTrans embeddedJmxTrans;

    private ResourceLoader resourceLoader;

    private boolean ignoreConfigurationNotFound = false;

    private String beanName = "jmxtrans";

    @Autowired
    public EmbeddedJmxTransFactory(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public SpringEmbeddedJmxTrans getObject() throws Exception {
        logger.info("Load JmxTrans with configuration '{}'", configurationUrls);
        if (embeddedJmxTrans == null) {

            if (configurationUrls == null) {
                configurationUrls = Collections.singletonList(DEFAULT_CONFIGURATION_URL);
            }
            ConfigurationParser parser = new ConfigurationParser();
            SpringEmbeddedJmxTrans newJmxTrans = new SpringEmbeddedJmxTrans();
            newJmxTrans.setObjectName("org.jmxtrans.embedded:type=EmbeddedJmxTrans,name=" + beanName);

            for (String delimitedConfigurationUrl : configurationUrls) {
                String[] tokens = StringUtils.commaDelimitedListToStringArray(delimitedConfigurationUrl);
                tokens = StringUtils.trimArrayElements(tokens);
                for (String configurationUrl : tokens) {
                    configurationUrl = configurationUrl.trim();
                    logger.debug("Load configuration {}", configurationUrl);
                    Resource configuration = resourceLoader.getResource(configurationUrl);
                    if (configuration.exists()) {
                        try {
                            parser.mergeEmbeddedJmxTransConfiguration(configuration.getInputStream(), newJmxTrans);
                        } catch (Exception e) {
                            throw new EmbeddedJmxTransException("Exception loading configuration " + configuration, e);
                        }
                    } else if (ignoreConfigurationNotFound) {
                        logger.debug("Ignore missing configuration file {}", configuration);
                    } else {
                        throw new EmbeddedJmxTransException("Configuration file " + configuration + " not found");
                    }
                }
            }
            embeddedJmxTrans = newJmxTrans;
            logger.info("Created EmbeddedJmxTrans with configuration {})", configurationUrls);
            embeddedJmxTrans.start();
        }
        return embeddedJmxTrans;
    }

    @Override
    public Class<?> getObjectType() {
        return EmbeddedJmxTrans.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * @param configurationUrl coma delimited list
     */
    public void setConfigurationUrl(String configurationUrl) {
        if (this.configurationUrls == null) {
            this.configurationUrls = new ArrayList<String>();
        }
        this.configurationUrls.add(configurationUrl);
    }

    public void setConfigurationUrls(List<String> configurationUrls) {
        if (this.configurationUrls == null) {
            this.configurationUrls = new ArrayList<String>();
        }
        this.configurationUrls.addAll(configurationUrls);
    }

    public void setIgnoreConfigurationNotFound(boolean ignoreConfigurationNotFound) {
        this.ignoreConfigurationNotFound = ignoreConfigurationNotFound;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
