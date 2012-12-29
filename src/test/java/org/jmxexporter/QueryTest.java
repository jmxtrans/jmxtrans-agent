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
package org.jmxexporter;

import org.jmxexporter.output.AbstractOutputWriter;
import org.jmxexporter.output.OutputWriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class QueryTest {

    static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    static ObjectName mockEdenSpacePool;
    static ObjectName mockPermGenPool;


    @BeforeClass
    public static void beforeClass() throws Exception {
        mockEdenSpacePool = new ObjectName("test:type=MemoryPool,name=PS Eden Space");
        mbeanServer.registerMBean(new MockMemoryPool("PS Eden Space", 87359488L), mockEdenSpacePool);
        mockPermGenPool = new ObjectName("test:type=MemoryPool,name=PS Perm Gen");
        mbeanServer.registerMBean(new MockMemoryPool("PS Perm Gen", 87752704L), mockPermGenPool);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mbeanServer.unregisterMBean(mockEdenSpacePool);
        mbeanServer.unregisterMBean(mockPermGenPool);
    }

    @Test
    public void basic_jmx_attribute_return_simple_result() throws Exception {
        JmxExporter jmxExporter = new JmxExporter();

        Query query = new Query("test:type=MemoryPool,name=PS Eden Space").addAttribute("CollectionUsageThreshold");
        jmxExporter.addQuery(query);
        query.collectMetrics();
        assertThat(query.getResults().size(), is(1));

        QueryResult result = query.getResults().poll();
        assertThat(result.getValue(), instanceOf(Number.class));
    }

    @Test
    public void test_composite_jmx_attribute() throws Exception {
        JmxExporter jmxExporter = new JmxExporter();

        Query query = new Query("test:type=MemoryPool,name=PS Perm Gen");
        jmxExporter.addQuery(query);
        query.addAttribute(new QueryAttribute("Usage", null, Arrays.asList("committed", "init", "max", "used")));
        query.collectMetrics();
        assertThat(query.getResults().size(), is(4));

        QueryResult result1 = query.getResults().poll();
        assertThat(result1.getValue(), instanceOf(Number.class));

        QueryResult result2 = query.getResults().poll();
        assertThat(result2.getValue(), instanceOf(Number.class));
    }

    @Test
    public void testExportResults() {
        JmxExporter jmxExporter = new JmxExporter();

        // CONFIGURE
        Query query = new Query("test:type=GarbageCollector,name=PS Scavenge");
        jmxExporter.addQuery(query);
        query.addAttribute("CollectionCount").addAttribute("CollectionTime");
        jmxExporter.addQuery(query);

        final AtomicInteger exportCount = new AtomicInteger();
        final AtomicInteger exportResultCount = new AtomicInteger();

        OutputWriter outputWriter = new AbstractOutputWriter() {
            @Override
            public void write(Iterable<QueryResult> results) {
                exportCount.incrementAndGet();
                for (QueryResult result : results) {
                    exportResultCount.incrementAndGet();
                }
            }
        };

        jmxExporter.getOutputWriters().add(outputWriter);
        assertThat(query.getOutputWriters().size(), is(0));
        assertThat(query.getEffectiveOutputWriters().size(), is(1));

        // PREPARE DATA
        long time = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
        for (int i = 0; i < 100; i++) {
            QueryResult result = new QueryResult("PS_Scavenge.GarbageCollector.CollectionTime", 5 * i, time);
            query.getResults().add(result);

            assertThat(query.getResults().size(), is(i + 1));


            time += TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
        }

        // TEST
        int actualExportResultCount = query.exportCollectedMetrics();
        assertThat(exportCount.get(), is(2));
        assertThat(exportResultCount.get(), is(100));
        assertThat(actualExportResultCount, is(100));
    }
}
