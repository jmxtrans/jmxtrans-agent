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
package org.jmxexporter.util.pool;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;


import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ManagedGenericKeyedObjectPoolTest {

    @Test
    public void testMbeanAttributeAccess() throws Exception {
        BaseKeyedPoolableObjectFactory<String, String> factory = new BaseKeyedPoolableObjectFactory<String, String>() {
            @Override
            public String makeObject(String key) throws Exception {
                return key;
            }
        };
        ManagedGenericKeyedObjectPool<String, String> objectPool = new ManagedGenericKeyedObjectPool<String, String>(factory);

        ObjectName objectName = new ObjectName("org.jmxexporter:Type=TestPool,Name=TestPool@" + System.identityHashCode(this));
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        objectName = mbeanServer.registerMBean(objectPool, objectName).getObjectName();
        try {
            Object numIdle = mbeanServer.getAttribute(objectName, "NumIdle");
            assertThat(numIdle, instanceOf(Number.class));

        } finally {
            mbeanServer.unregisterMBean(objectName);
        }



    }
}
