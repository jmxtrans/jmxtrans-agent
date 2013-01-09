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
package org.jmxtrans.embedded.spring;

import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.config.ConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jmx.export.naming.SelfNaming;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class EmbeddedJmxTransFactory implements FactoryBean<EmbeddedJmxTrans>, DisposableBean, BeanNameAware, SelfNaming {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> configurationUrls =
            Arrays.asList("classpath:jmxtrans.json", "classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json");

    private String name;

    private EmbeddedJmxTrans embeddedJmxTrans;

    @PostConstruct
    @Override
    public EmbeddedJmxTrans getObject() throws Exception {
        logger.info("Load JmxTrans with configuration '{}'", configurationUrls);
        if (embeddedJmxTrans == null) {
            embeddedJmxTrans = new ConfigurationParser().newEmbeddedJmxTrans(configurationUrls);
            logger.info("Start JmxTrans with configuration {})", configurationUrls);
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
     * @param configurationUrl delimited list of configuration urls (',', ';', '\n')
     */
    public void setConfigurationUrl(String configurationUrl) {
        this.configurationUrls = delimitedStringToList(configurationUrl);
    }

    public void setConfigurationUrls(List<String> configurationUrls) {
        this.configurationUrls = configurationUrls;
    }

    protected List<String> delimitedStringToList(String configurationUrl) {
        configurationUrl = configurationUrl.replaceAll(";", ",");
        configurationUrl = configurationUrl.replaceAll("\n", ",");
        String[] arr = configurationUrl.split(",");
        List<String> result = new ArrayList<String>();
        for (String str : arr) {
            str = str.trim();
            if (!str.isEmpty()) {
                result.add(str);
            }
        }
        return result;
    }

    @PreDestroy
    @Override
    public void destroy() throws Exception {
        if (embeddedJmxTrans != null) {
            embeddedJmxTrans.stop();
        }
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    @Override
    public ObjectName getObjectName() throws MalformedObjectNameException {
        return new ObjectName("org.jmxtrans.embedded:type=EmbeddedJmxTrans,name=" + name);
    }
}
