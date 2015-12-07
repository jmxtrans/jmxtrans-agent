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
import java.util.*;
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

    /**
     * The attribute(s) to retrieve ({@link MBeanServer#getAttribute(javax.management.ObjectName, String)})
     * 
     * If empty, will fetch all attributes of the MBean.
     */
    @Nonnull
    protected List<String> attributes;
    @Nullable
    protected final String resultAlias;
    /**
     * If the MBean attribute value is a {@link CompositeData}, the key to lookup.
     *
     * @see CompositeData#get(String)
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
        this(objectName, attribute, null, null, null, null, resultNameStrategy);
    }

    /**
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nullable String attribute, int position, @Nonnull ResultNameStrategy resultNameStrategy) {
        this(objectName, attribute, null, position, null, null, resultNameStrategy);
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
        this(objectName, nullOrEmtpy(attribute) ? Collections.<String>emptyList() : Collections.singletonList(attribute), key, position,
                type, resultAlias, resultNameStrategy);
    }

    private static boolean nullOrEmtpy(String attribute) {
        return attribute == null || attribute.isEmpty();
    }
    
    /**
     * Creates a query that accepts a list of attributes to collect. If the list is empty, all attributes will be collected.
     * 
     * @see #Query(String, String, String, Integer, String, String, ResultNameStrategy)
     */
    public Query(@Nonnull String objectName, @Nonnull List<String> attributes, @Nullable String key, @Nullable Integer position,
            @Nullable String type, @Nullable String resultAlias, @Nonnull ResultNameStrategy resultNameStrategy) {
        try {
            this.objectName = new ObjectName(Preconditions2.checkNotNull(objectName));
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid objectName '" + objectName + "'", e);
        }
        this.attributes = Collections.unmodifiableList(new ArrayList<String>(Preconditions2.checkNotNull(attributes, "attributes")));
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
            collectAndExportForObjectName(mbeanServer, outputWriter, on);
        }
    }

    private void collectAndExportForObjectName(MBeanServer mbeanServer, OutputWriter outputWriter, ObjectName on) {
        for (String attribute : resolveAttributes(mbeanServer, on)) {
            collectAndExportAttribute(mbeanServer, outputWriter, on, attribute);
        }
    }


    private List<String> resolveAttributes(MBeanServer mbeanServer, ObjectName on) {
        if (attributes.isEmpty()) {
            return findAllAttributes(mbeanServer, on);
        }
        return attributes;
    }

    private List<String> findAllAttributes(MBeanServer mbeanServer, ObjectName on) {
        List<String> resolvedAttributes = new ArrayList<>();
        // Null or empty attribute specified, collect all attributes
        try {
            for (MBeanAttributeInfo mBeanAttributeInfo : mbeanServer.getMBeanInfo(on).getAttributes()) {
                resolvedAttributes.add(mBeanAttributeInfo.getName());
            }
        } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
            logger.log(Level.WARNING, "Error when finding attributes for ObjectName " + on + ", all attributes will not be collected", e);
        }
        return resolvedAttributes;
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
                    // Get for all keys
                    CompositeType compositeType = compositeData.getCompositeType();
                    for (String key : compositeType.keySet()) {
                        value = compositeData.get(key);
                        processAttributeValue(outputWriter, objectName, attribute, key, value);
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

            processAttributeValue(outputWriter, objectName, attribute, key, value);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception collecting " + objectName + "#" + attribute + (key == null ? "" : "#" + key), e);
        }
    }

    /**
     *
     * @param outputWriter
     * @param objectName
     * @param attribute attribute of the MBean
     * @param compositeDataKey if the MBean value is a {@link CompositeData}, the name of the key (see {@link CompositeData#get(String)})
     * @param value value
     * @throws IOException
     */
    private void processAttributeValue(@Nonnull OutputWriter outputWriter, @Nonnull ObjectName objectName, @Nonnull String attribute,
                                       @Nullable String compositeDataKey, Object value) throws IOException {

        if (value instanceof Iterable) {
            Iterable valueAsIterable = (Iterable) value;
            if (position == null) {
                // get for all entries
                int idx = 0;
                for (Object subValue : valueAsIterable) {
                    String resultName = resultNameStrategy.getResultName(this, objectName, attribute, compositeDataKey, idx);
                    outputWriter.writeQueryResult(resultName, type, subValue);
                    idx++;
                }
            } else {
                String resultName = resultNameStrategy.getResultName(this, objectName, attribute, compositeDataKey, position);
                value = Iterables2.get((Iterable) value, position);
                outputWriter.writeQueryResult(resultName, type, value);
            }
        } else {
            String resultName = resultNameStrategy.getResultName(this, objectName, attribute, compositeDataKey, null);
            outputWriter.writeQueryResult(resultName, type, value);
        }
    }

    @Override
    public String toString() {
        return "Query{" +
                "objectName=" + objectName +
                ", resultAlias='" + resultAlias + '\'' +
                ", attributes='" + attributes + '\'' +
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
    public List<String> getAttributes() {
        return attributes;
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
