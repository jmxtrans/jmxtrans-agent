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

import java.util.HashMap;
import java.util.Map;
import javax.management.openmbean.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 * @see java.lang.management.MemoryPoolMXBean
 */
public class Mock implements MockMBean {

    /**
     * @see java.lang.management.MemoryUsage
     */
    private CompositeData usage;
    private String name;

    /**
     * @param name      e.g. PS Eden Space
     * @param committed e.g. 87359488L
     */
    public Mock(String name, long committed) {
        this.name = name;
        try {
            CompositeType type = new CompositeType(
                    "java.lang.management.MemoryUsage", "My Type Desc",
                    new String[]{"committed", "init", "max", "used"},
                    new String[]{"committed", "init", "max", "used"},
                    new OpenType[]{SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG});
            usage = new CompositeDataSupport(
                    type,
                    new String[]{"committed", "init", "max", "used"},
                    new Long[]{committed, 16318464L, 88997888L, 87359488L});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompositeData getUsage() {
        return usage;
    }

    @Override
    public long getCollectionUsageThreshold() {
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int[] getIntArray() {
        return new int[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    public Integer[] getIntegerArray() {
        return new Integer[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    public List<Integer> getIntegerList() {
        return Arrays.asList(0, 1, 2, 3, 4, 5);
    }

    @Override
    public Map<String, Double> getStringMap() {
        Map<String, Double> results = new HashMap<>();
        results.put("foo", 0.01);
        results.put("bar", 1.0);
        return results;
    }

    @Override
    public Map<Double, Double> getDoubleMap() {
        Map<Double, Double> results = new HashMap<>();
        results.put(0.01, 1.0);
        results.put(0.02, 2.0);
        results.put(0.03, 3.0);
        return results;
    }
}
