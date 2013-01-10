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
package org.jmxtrans.embedded.util.jmx;

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
