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
package org.jmxexporter.util.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * JMX utils.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class JmxUtils2 {
    private final static Logger logger = LoggerFactory.getLogger(JmxUtils2.class);

    private JmxUtils2() {

    }

    /**
     * Try to register given <code>object</code> with <code>objectName</code>.
     * <p/>
     * If registration fails, a {@link Logger#warn(String)} message is emitted and <code>null</code> is returned.
     *
     * @param object
     * @param objectName
     * @param mbeanServer
     * @return the ObjectName of the registered object or <code>null</code> if registration failed.
     */
    @Nullable
    public static ObjectName registerObject(Object object, String objectName, MBeanServer mbeanServer) {
        try {
            return mbeanServer.registerMBean(object, new ObjectName(objectName)).getObjectName();
        } catch (Exception e) {
            logger.warn("Failure to register {}:{}", objectName, object, e);
            return null;
        }
    }

    /**
     * Try to unregister given <code>objectName</code>.
     * <p/>
     * If given <code>objectName</code> is <code>null</code>, nothing is done.
     * If registration fails, a {@link Logger#warn(String)} message is emitted and <code>null</code> is returned.
     *
     * @param objectName  objectName to unregister
     * @param mbeanServer MBeanServer to which the objectName is unregistered
     */
    public static void unregisterObject(ObjectName objectName, MBeanServer mbeanServer) {
        if (objectName == null) {
            return;
        }
        try {
            mbeanServer.unregisterMBean(objectName);
        } catch (Exception e) {
            logger.warn("Failure to unregister {}", objectName, e);
        }
    }
}
