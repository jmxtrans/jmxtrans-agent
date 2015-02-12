/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for an {@link EmbeddedJmxTransFactory}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class EmbeddedJmxTransBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private static final String CONFIGURATION_ATTRIBUTE = "configuration";
    private static final String CONFIGURATION_SCAN_PERIOD_IN_SECONDS = "configuration-scan-period-in-seconds";
    private static final String IGNORE_CONFIGURATION_NOT_FOUND_ATTRIBUTE = "ignore-configuration-not-found";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected Class getBeanClass(Element element) {
        return EmbeddedJmxTransFactory.class;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        String id = element.getAttribute(ID_ATTRIBUTE);
        return (StringUtils.hasText(id) ? id : "jmxtrans");
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.setRole(BeanDefinition.ROLE_APPLICATION);
        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));

        if (element.hasAttribute(CONFIGURATION_SCAN_PERIOD_IN_SECONDS)) {
            builder.addPropertyValue("configurationScanPeriodInSeconds", element.getAttribute(CONFIGURATION_SCAN_PERIOD_IN_SECONDS));
        }
        
        if (element.hasAttribute(IGNORE_CONFIGURATION_NOT_FOUND_ATTRIBUTE)) {
            builder.addPropertyValue("ignoreConfigurationNotFound", element.getAttribute(IGNORE_CONFIGURATION_NOT_FOUND_ATTRIBUTE));
        }
        List<String> configurationUrls = new ArrayList<String>();
        if (element.hasAttribute(CONFIGURATION_ATTRIBUTE)) {
            String configurationUrl = element.getAttribute(CONFIGURATION_ATTRIBUTE);
            logger.debug("Add configuration from attribute {}", configurationUrl);
            configurationUrls.add(configurationUrl);
        }

        NodeList configurationNodeList = element.getElementsByTagNameNS(element.getNamespaceURI(), CONFIGURATION_ATTRIBUTE);
        for (int i = 0; i < configurationNodeList.getLength(); i++) {
            Node node = configurationNodeList.item(i);
            if (node instanceof Element) {
                String configurationUrl = node.getTextContent();
                logger.debug("Add configuration from attribute {}", configurationUrl);
                configurationUrls.add(configurationUrl);
            } else {
                throw new EmbeddedJmxTransException("Invalid configuration child element " + node);
            }

        }
        builder.addPropertyValue("configurationUrls", configurationUrls);
    }
}
