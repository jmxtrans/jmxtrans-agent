/*
 * Copyright (c) 2010-2016 the original author or authors
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
package org.jmxtrans.agent.zabbix;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;

import org.jmxtrans.agent.DiscoveryQuery;
import org.jmxtrans.agent.ResultNameStrategyImpl;
import org.jmxtrans.agent.util.json.Json;
import org.jmxtrans.agent.util.json.JsonArray;
import org.jmxtrans.agent.util.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;


/**
 * DiscoveryQueryTest
 * 
 * @author Steve McDuff
 */
public class DiscoveryQueryTest
{
    public DiscoveryQueryTest()
    {
        super();
    }

    /**
     * Extract the Garbage Collector JMX data. Validate the format
     * Of the Zabbix discovery output.
     * @throws Exception
     */
    @Test
    public void fetchDiscoveryQuery() throws Exception
    {
        TestOutputWriter output = new TestOutputWriter();
        ResultNameStrategyImpl strategy =  new ResultNameStrategyImpl();
        List<String> attributes  = new ArrayList<>();
        attributes.add("name");
        attributes.add("type");
        
        DiscoveryQuery query = new DiscoveryQuery("java.lang:type=GarbageCollector,name=*", attributes, null, null, null, "discovery[garbageCollector]", strategy, 200);
        

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        query.collectAndExport(mbeanServer, output);
        
        Assert.assertNotNull(output.lastValue);
        Assert.assertEquals("discovery[garbageCollector]",output.lastMetricName);
        
        JsonObject parse = (JsonObject) Json.parse((String)output.lastValue);
        JsonArray data = (JsonArray) parse.get("data");
        Assert.assertTrue("Must have at least 2 garbage collectors", data.size() >=2 );
        
        JsonObject gc1 = (JsonObject) data.get(0);
        String name1 = gc1.getString("{#NAME}", null);
        Assert.assertNotNull(name1);
        String type1 = gc1.getString("{#TYPE}", null);
        Assert.assertEquals("GarbageCollector",type1);
        
        
        JsonObject gc2 = (JsonObject) data.get(0);
        String name2 = gc2.getString("{#NAME}", null);
        Assert.assertNotNull(name2);
        String type2 = gc1.getString("{#TYPE}", null);
        Assert.assertEquals("GarbageCollector",type2);
        
    }
    
    @Test
    public void testFormatValue() {
        
        ResultNameStrategyImpl strategy =  new ResultNameStrategyImpl();
        List<String> attributes  = new ArrayList<>();
        attributes.add("name");
        attributes.add("type");
        
        DiscoveryQuery query = new DiscoveryQuery("java.lang:type=GarbageCollector,name=*", attributes, null, null, null, "discovery[garbageCollector]", strategy, 200);
        
        String formatDiscoveryValue = query.formatDiscoveryValue("\"Operation : public void my.package.MyClass(my.package.String value, my.other.Value other) throws somethign, and.another\"");
        
        Assert.assertEquals("Operation___public_void_my_package_MyClass_my_package_String_value__my_other_Value_other__throws_somethign__and_another", formatDiscoveryValue);

        
    }
    
}
