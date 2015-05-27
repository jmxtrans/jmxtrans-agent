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

import org.jmxtrans.agent.util.Preconditions2;
import org.jmxtrans.agent.util.collect.Iterables2;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Query {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Nonnull
    protected ResultNameStrategy resultNameStrategy;

    @Nonnull
    protected final ObjectName objectName;

    @Nullable
    protected final String resultAlias;
    /**
     * The attribute to retrieve ({@link MBeanServer#getAttribute(javax.management.ObjectName, String)})
     */
    @Nullable
    protected final String attribute;
    /**
     * If the MBean attribute value is a {@link CompositeData}, the key to lookup.
     */
    @Nullable
    protected final String key;
    /**
     * If the returned value is a {@link java.util.Collection}or an array, the position of the entry to lookup.
     */
    @Nullable
    protected final Integer position;
    /**
     * Attribute type like '{@code gauge}' or '{@code counter}'. Used by monitoring systems like Librato who require this information.
     */
    @Nullable
    private String type;

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nullable String attribute, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, null, null, attribute, resultNameStrategy);
    }

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nullable String attribute, int position, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, position, null, attribute, resultNameStrategy);
    }

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nullable String attribute, @Nullable String resultAlias, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, null, null, resultAlias, resultNameStrategy);
    }

    /**
     * @param objectName         The {@link ObjectName} to search for
     *                           ({@link MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)}),
     *                           can contain wildcards and return several entries.
     * @param attribute          The attribute to retrieve ({@link MBeanServer#getAttribute(javax.management.ObjectName, String)})
     * @param key                if the MBean attribute value is a {@link CompositeData}, the key to lookup.
     * @param position           if the returned value is a {@link java.util.Collection} or an array, the position of the entry to lookup.
     * @param type               type of the metric ('counter', 'gauge', ...)
     * @param resultAlias
     * @param resultNameStrategy the {@link org.jmxtrans.agent.ResultNameStrategy} used during the
     *                           {@link #collectAndExport(javax.management.MBeanServer, OutputWriter)} phase.
     */
    public Query(@Nonnull String objectName, @Nullable String attribute, @Nullable String key, @Nullable Integer position,
                 @Nullable String type, @Nullable String resultAlias, @Nonnull ResultNameStrategy resultNameStrategy) {
        try {
            this.objectName = new ObjectName(Preconditions2.checkNotNull(objectName));
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid objectName '" + objectName + "'", e);
        }
        this.attribute = attribute;
        this.key = key;
        this.resultAlias = resultAlias;
        this.position = position;
        this.type = type;
        this.resultNameStrategy = Preconditions2.checkNotNull(resultNameStrategy, "resultNameStrategy");
    }

    public void collectAndExport(@Nonnull MBeanServer mbeanServer, @Nonnull OutputWriter outputWriter) {
        if (resultNameStrategy == null)
            throw new IllegalStateException("resultNameStrategy is not defined, query object is not properly initialized");

        Set<ObjectName> objectNames = mbeanServer.queryNames(objectName, null);

        for (ObjectName on : objectNames) {
            try {
                if (attribute == null || attribute.isEmpty()) {
                    for (MBeanAttributeInfo mBeanAttributeInfo : mbeanServer.getMBeanInfo(on).getAttributes()) {
                        collectAndExportAttribute(mbeanServer, outputWriter, on, mBeanAttributeInfo.getName());
                    }

                } else {
                    collectAndExportAttribute(mbeanServer, outputWriter, on, attribute);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception collecting " + on + "#" + attribute + (key == null ? "" : "#" + key), e);
            }
        }
    }

    private void collectAndExportAttribute(MBeanServer mbeanServer, OutputWriter outputWriter, ObjectName objectName, String attribute) {
        try {
            Object attributeValue = null;
            try {
                attributeValue = mbeanServer.getAttribute(objectName, attribute);
            } catch (Exception ex) {
                logger.warning("Failed to fetch attribute for '" + objectName + "'#" + attribute + ", exception: " + ex.getMessage());
                return;
            }

            Object value;
            if (attributeValue instanceof CompositeData) {
                CompositeData compositeData = (CompositeData) attributeValue;
                if (key == null) {
                    /** Get for all keys */
                    CompositeType compositeType = compositeData.getCompositeType();
                    for (String key : compositeType.keySet()) {
                        value = compositeData.get(key);
                        processAttributeValue(outputWriter, objectName, attribute, value, key);
                    }
                    return;
                } else {
                    value = compositeData.get(key);
                }
            } else {
                if (key == null) {
                    value = attributeValue;
                } else {
                    logger.warning("Ignore NON compositeData for specified key for '" + objectName +
                            "'#" + attribute + "#" + key + ": " + attributeValue);
                    return;
                }
            }
            if (value != null && value.getClass().isArray()) {
                List valueAsList = new ArrayList();
                for (int i = 0; i < Array.getLength(value); i++) {
                    valueAsList.add(Array.get(value, i));
                }
                value = valueAsList;
            }

            processAttributeValue(outputWriter, objectName, attribute, value, key);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception collecting " + objectName + "#" + attribute + (key == null ? "" : "#" + key), e);
        }
    }

    private void processAttributeValue(OutputWriter outputWriter, ObjectName objectName, String attribute,
                                       Object value, String key) throws IOException {
        String resultName = resultNameStrategy.getResultName(this, objectName, key, attribute);
        if (value instanceof Iterable) {
            Iterable iterable = (Iterable) value;
            if (position == null) {
                int idx = 0;
                for (Object entry : iterable) {
                    outputWriter.writeQueryResult(resultName + "_" + idx++, type, entry);
                }
            } else {
                value = Iterables2.get((Iterable) value, position);
                outputWriter.writeQueryResult(resultName, type, value);
            }
        } else {
            outputWriter.writeQueryResult(resultName, type, value);
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

    @Nonnull
    public ObjectName getObjectName() {
        return objectName;
    }

    @Nullable
    public String getResultAlias() {
        return resultAlias;
    }

    @Nullable
    public String getAttribute() {
        return attribute;
    }

    @Nullable
    public String getKey() {
        return key;
    }

    @Nullable
    public Integer getPosition() {
        return position;
    }

    @Nullable
    public String getType() {
        return type;
    }
}
