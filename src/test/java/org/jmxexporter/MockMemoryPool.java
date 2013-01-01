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
package org.jmxexporter;

import javax.management.openmbean.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 * @see java.lang.management.MemoryPoolMXBean
 */
public class MockMemoryPool implements MockMemoryPoolMBean {

    /**
     * @see java.lang.management.MemoryUsage
     */
    private CompositeData usage;

    private String name;

    /**
     * @param name      e.g. PS Eden Space
     * @param committed e.g. 87359488L
     */
    public MockMemoryPool(String name, long committed) {
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
}
