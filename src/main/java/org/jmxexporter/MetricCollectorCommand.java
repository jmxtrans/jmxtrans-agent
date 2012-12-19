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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Set;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class MetricCollectorCommand implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    MBeanServer mbeanServer;

    Query query;

    MetricCollector metricCollector;

    @Override
    public void run() {
        try {
            Set<ObjectName> objectNames = mbeanServer.queryNames(query.getObjectName(), null);
            for (ObjectName objectName : objectNames) {
                long epoch = System.currentTimeMillis();
                AttributeList attributes = mbeanServer.getAttributes(objectName, query.getAttributeNames());
                for (Attribute attribute : attributes.asList()) {
                    query.addResult(metricCollector.getResult(epoch, attribute));
                }
            }
        } catch (Exception e) {
            logger.warn("error collecting metrics for " + query, e);
        }
    }
}
