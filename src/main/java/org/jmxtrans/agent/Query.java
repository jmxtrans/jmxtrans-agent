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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Query {

    @Nonnull
    protected final ObjectName objectName;
    @Nonnull
    protected final String resultAlias;
    @Nonnull
    protected final String attribute;
    @Nullable
    protected final String key;
    @Nullable
    protected final Integer position;
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * @see #Query(String, String, String, Integer, String)
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute) {
        this(objectName, attribute, null, null, attribute);
    }

    /**
     * @see #Query(String, String, String, Integer, String)
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute, int position) {
        this(objectName, attribute, null, position, attribute);
    }

    /**
     * @see #Query(String, String, String, Integer, String)
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute, @Nonnull String resultAlias) {
        this(objectName, attribute, null, null, resultAlias);
    }

    /**
     * @param objectName  The {@link ObjectName} to search for
     *                    ({@link MBeanServer#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)}),
     *                    can contain wildcards and return several entries.
     * @param attribute   The attribute to retrieve ({@link MBeanServer#getAttribute(javax.management.ObjectName, String)})
     * @param key         if the MBean attribute value is a {@link CompositeData}, the key to lookup.
     * @param position    if the returned value is a {@link java.util.Collection}, the position of the entry to lookup.
     * @param resultAlias
     */
    public Query(@Nonnull String objectName, @Nonnull String attribute, @Nullable String key, @Nullable Integer position, @Nonnull String resultAlias) {
        try {
            this.objectName = new ObjectName(Preconditions2.checkNotNull(objectName));
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid objectName '" + objectName + "'", e);
        }
        this.attribute = Preconditions2.checkNotNull(attribute);
        this.key = key;
        this.resultAlias = Preconditions2.checkNotNull(resultAlias);
        this.position = position;
    }

    public void collectAndExport(@Nonnull MBeanServer mbeanServer, @Nonnull OutputWriter outputWriter) {

        Set<ObjectName> objectNames = mbeanServer.queryNames(objectName, null);

        for (ObjectName on : objectNames) {

            try {
                Object attributeValue = mbeanServer.getAttribute(on, attribute);

                Object value;
                if (attributeValue instanceof CompositeData) {
                    CompositeData compositeData = (CompositeData) attributeValue;
                    if (key == null) {
                        logger.warning("Ignore compositeData without key specified for '" + on + "'#" + attribute + ": " + attributeValue);
                        continue;
                    } else {
                        value = compositeData.get(key);
                    }
                } else {
                    if (key == null) {
                        value = attributeValue;
                    } else {
                        logger.warning("Ignore NON compositeData for specified key for '" + on + "'#" + attribute + "#" + key + ": " + attributeValue);
                        continue;
                    }
                }
                if (value != null && value.getClass().isArray()) {
                    List valueAsList = new ArrayList();
                    for (int i = 0; i < Array.getLength(value); i++) {
                        valueAsList.add(Array.get(value, i));
                    }
                    value = valueAsList;
                }
                if (value instanceof Iterable) {
                    Iterable iterable = (Iterable) value;
                    if (position == null) {
                        int idx = 0;
                        for (Object entry : iterable) {
                            outputWriter.writeQueryResult(resultAlias + "_" + idx++, entry);
                        }
                    } else {
                        value = Iterables2.get((Iterable) value, position);
                        outputWriter.writeQueryResult(resultAlias, value);
                    }
                } else {
                    outputWriter.writeQueryResult(resultAlias, value);
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
