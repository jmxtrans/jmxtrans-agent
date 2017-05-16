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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jmxtrans.agent.util.StringUtils2;
import org.jmxtrans.agent.util.json.JsonArray;
import org.jmxtrans.agent.util.json.JsonObject;
import org.jmxtrans.agent.util.logging.Logger;

/**
 * DiscoveryQuery : Used to discover a list of JMX beans matching a specific naming pattern.
 * Used with the Zabbix server.
 * 
 * <p>
 * For example, the following discovery rule :
 * 
 * <pre>
 * {@code}
 *  &lt;discoveryQuery 
 *    objectName="java.lang:type=GarbageCollector,name=*" 
 *    attributes="name,type" 
 *    resultAlias="discovery[garbageCollector]" 
 *    collectIntervalInSeconds="300"/&gt;
 *    
 *  &lt;query objectName="java.lang:type=GarbageCollector,name=*"
 *    attributes="CollectionTime,CollectionCount"
 *    resultAlias="discovery[GarbageCollector.%name%.#attribute#]" /&gt;
 * </pre>
 * 
 * May yield the following discovery output (formatted for readability) :
 * 
 * <pre>
 * {@code}
 * {"data":[
 *   {"{#name}":"PS Scavenge","{#type}":"GarbageCollector"},
 *   {"{#name}":"PS MarkSweep","{#type}":"GarbageCollector"}
 * ]}
 * </pre>
 * 
 * On the Zabbix side, create a "Discovery Rule" of type "Zabbix trapper"
 * with a "Key" that matches the result alias. You can then create "Item prototypes" that use the values.
 * 
 * Sample Zabbix configuration that matches the example above :
 * 
 * <pre>
 * {@code}
 * Discovery rule : 
 *   Name : Discover Garbage Collectors
 *   Key : discovery[garbageCollector]
 *   
 * Item Prototype
 *   Name : Object {#TYPE} named {#NAME}
 *   Key : discovery[{#TYPE}.{#NAME}.CollectionTime]
 * 
 * Item Prototype
 *   Name : Object {#TYPE} named {#NAME}
 *   Key : discovery[{#TYPE}.{#NAME}.CollectionCount]
 * 
 * </pre>
 * 
 * NOTE : It can take a few minutes for Zabbix to enable newly created discovery
 * rules and item prototypes.
 * 
 * @author Steve McDuff
 */
public class DiscoveryQuery extends Query
{
    private final Logger logger = Logger.getLogger(getClass().getName());

    public DiscoveryQuery(String objectName, List<String> attributes, String key, Integer position, String type,
        String resultAlias, ResultNameStrategy resultNameStrategy, Integer collectInterval)
    {
        super(objectName, attributes, key, position, type, resultAlias, resultNameStrategy, collectInterval);
    }

    @Override
    public void collectAndExport(MBeanServer mbeanServer, OutputWriter outputWriter)
    {
        if (resultNameStrategy == null)
            throw new IllegalStateException(
                "resultNameStrategy is not defined, query object is not properly initialized");

        try
        {
            Set<ObjectName> objectNames = mbeanServer.queryNames(objectName, null);

            String discoveryResult = formatDiscoveryValue(objectNames);

            String resultName = resultNameStrategy.getResultName(this, objectName, null, null, null);
            String type = getType();
            outputWriter.writeQueryResult(resultName, type, discoveryResult);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "DiscoveryQuery. Exception collecting " + objectName + "#" + getAttributes() +
                (key == null ? "" : "#" + key), ex);
        }
    }

    private String formatDiscoveryValue(Set<ObjectName> objectNames)
    {
        JsonObject result = new JsonObject();
        JsonArray data = new JsonArray();
        result.add("data", data);

        for (ObjectName on : objectNames)
        {
            try
            {
                JsonObject discoveredObject = new JsonObject();
                for (String attribute : attributes)
                {
                    // Generate the discovered property with the format {"{#NAME}":"value"}
                    String keyProperty = on.getKeyProperty(attribute);
                    // skip nulls
                    if (keyProperty != null)
                    {
                        String formattedKey = formatDiscoveryKey(attribute);
                        String formattedValue = formatDiscoveryValue(keyProperty);
                        discoveredObject.add(formattedKey, formattedValue);
                    }
                }
                data.add(discoveredObject);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "DiscoveryQuery.formatDiscoveryValue. Exception collecting " + objectName +
                    "#" + getAttributes() + (key == null ? "" : "#" + key), ex);
            }
        }

        String discoveryResult = result.toString();
        return discoveryResult;
    }

    public String formatDiscoveryValue(String keyProperty)
    {
        // transform the property values to match the way JMXTransAgent 
        // will format them in the default naming strategy.
        StringBuilder builder = new StringBuilder();
        StringUtils2.appendEscapedNonAlphaNumericChars(keyProperty, builder);
        keyProperty = builder.toString();
        return keyProperty;
    }

    private String formatDiscoveryKey(String attribute)
    {
        return "{#" + attribute.toUpperCase() + "}";
    }

}
