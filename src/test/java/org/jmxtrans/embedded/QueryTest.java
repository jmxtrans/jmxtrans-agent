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
package org.jmxtrans.embedded;

import org.jmxtrans.embedded.output.AbstractOutputWriter;
import org.jmxtrans.embedded.output.OutputWriter;
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
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();

        Query query = new Query("test:type=MemoryPool,name=PS Eden Space").addAttribute("CollectionUsageThreshold");
        embeddedJmxTrans.addQuery(query);
        query.collectMetrics();
        assertThat(query.getResults().size(), is(1));

        QueryResult result = query.getResults().poll();
        assertThat(result.getValue(), instanceOf(Number.class));
    }

    @Test
    public void test_composite_jmx_attribute() throws Exception {
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();

        Query query = new Query("test:type=MemoryPool,name=PS Perm Gen");
        embeddedJmxTrans.addQuery(query);
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
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();

        // CONFIGURE
        Query query = new Query("test:type=GarbageCollector,name=PS Scavenge");
        embeddedJmxTrans.addQuery(query);
        query.addAttribute("CollectionCount").addAttribute("CollectionTime");
        embeddedJmxTrans.addQuery(query);

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

        embeddedJmxTrans.getOutputWriters().add(outputWriter);
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

    @Test
    public void testDisabledOutputWriter(){
        EmbeddedJmxTrans embeddedJmxTrans = new EmbeddedJmxTrans();

        // CONFIGURE
        Query query = new Query("test:type=GarbageCollector,name=PS Scavenge");
        embeddedJmxTrans.addQuery(query);
        query.addAttribute("CollectionCount").addAttribute("CollectionTime");
        embeddedJmxTrans.addQuery(query);

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
        outputWriter.setEnabled(false);

        embeddedJmxTrans.getOutputWriters().add(outputWriter);
        assertThat(query.getOutputWriters().size(), is(0));
        assertThat(query.getEffectiveOutputWriters().size(), is(0));
    }
}
