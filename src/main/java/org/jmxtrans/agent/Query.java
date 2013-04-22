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
package org.jmxtrans.agent;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Query {
    private final Logger logger = Logger.getLogger(getClass().getName());
    protected final ObjectName objectName;
    protected final String resultAlias;
    protected final String attribute;
    protected final String key;

    public Query(String objectName, String attribute, String resultAlias) {
        this(objectName, attribute, null, resultAlias);
    }

    public Query(String objectName, String attribute, String key, String resultAlias) {
        try {
            this.objectName = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid objectName '" + objectName + "'", e);
        }
        this.attribute = attribute;
        this.key = key;
        this.resultAlias = resultAlias;
    }

    public void collectAndExport(MBeanServer mbeanServer, OutputWriter outputWriter) {

        Set<ObjectName> objectNames = mbeanServer.queryNames(objectName, null);

        for (ObjectName on : objectNames) {

            try {
                Object attributeValue = mbeanServer.getAttribute(on, attribute);

                if (attributeValue instanceof CompositeData) {
                    CompositeData compositeData = (CompositeData) attributeValue;
                    if (key == null) {
                        logger.warning("Ignore compositeData without key specified for '" + on + "'#" + attribute + ": " + attributeValue);
                    } else {
                        outputWriter.write(resultAlias, compositeData.get(key));
                    }
                } else {
                    if (key == null) {
                        outputWriter.write(resultAlias, attributeValue);
                    } else {
                        logger.warning("Ignore NON compositeData for specified key for '" + on + "'#" + attribute + "#" + key + ": " + attributeValue);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception collecting " + on + "#" + attribute + (key == null ? "" : "#" + key), e);
            }
        }
    }

    @Override
    public String toString() {
        return "Query{" +
                "objectName=" + objectName +
                ", resultAlias='" + resultAlias + '\'' +
                ", attribute='" + attribute + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
