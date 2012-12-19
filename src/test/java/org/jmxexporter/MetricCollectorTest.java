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

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class MetricCollectorTest {
    static MBeanServer mbeanServer;
    MetricCollector metricCollector = new MetricCollector();

    @BeforeClass
    static public void setUpClass() throws Exception {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    @Test
    public void basic_jmx_attribute_return_simple_result() throws Exception {
        Attribute attribute = getMbeanAttribute("java.lang:type=MemoryPool,name=PS Eden Space", "CollectionUsageThreshold");
        assertTrue("Not a number " + attribute, attribute.getValue() instanceof Number);
        Result result = metricCollector.getResult(0L, attribute);
    }

    @Test
    public void array_jmx_attribute_return_simple_result() throws Exception {
        Attribute attribute = getMbeanAttribute("java.lang:type=Threading", "AllThreadIds");
        assertTrue("Not a array " + attribute, attribute.getValue().getClass().isArray());
        Result result = metricCollector.getResult(0L, attribute);
        System.out.println(attribute);
    }

    private Attribute getMbeanAttribute(String objectName, String attributeName) throws InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
        return mbeanServer.getAttributes(new ObjectName(objectName), new String[]{attributeName}).asList().get(0);
    }
}
