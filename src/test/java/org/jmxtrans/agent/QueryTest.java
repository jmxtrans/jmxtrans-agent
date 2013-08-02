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

import org.junit.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class QueryTest {
    static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    static ObjectName mockObjectName;
    static Mock mock = new Mock("PS Eden Space", 87359488L);
    MockOutputWriter mockOutputWriter = new MockOutputWriter();

    @BeforeClass
    public static void beforeClass() throws Exception {
        mockObjectName = new ObjectName("test:type=Mock,name=mock");
        mbeanServer.registerMBean(mock, mockObjectName);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mbeanServer.unregisterMBean(mockObjectName);
    }

    @Before
    public void before() {
        mockOutputWriter = new MockOutputWriter();
    }

    @After
    public void after() {
        mockOutputWriter = null;
    }

    @Test
    public void basic_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "CollectionUsageThreshold");
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void indexed_list_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntegerList", 1);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("IntegerList");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void non_indexed_list_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntegerList");
        query.collectAndExport(mbeanServer, mockOutputWriter);

        for (int i = 0; i < mock.getIntegerList().size(); i++) {
            String name = "IntegerList_" + i;
            Object actual = mockOutputWriter.resultsByName.get(name);
            assertThat("Result '" + name + "' is missing", actual, notNullValue());
            assertThat("Result '" + name + "' type is invalid", actual, instanceOf(Number.class));
        }

    }

    @Test
    public void indexed_int_array_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntArray", 1);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("IntArray");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void indexed_integer_array_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntegerArray", 1);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("IntegerArray");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    public static class MockOutputWriter extends AbstractOutputWriter {

        protected final boolean failOnDuplicateResult;
        protected final Map<String, Object> resultsByName = new HashMap<String, Object>();

        public MockOutputWriter() {
            this(true);
        }

        public MockOutputWriter(boolean failOnDuplicateResult) {
            this.failOnDuplicateResult = failOnDuplicateResult;
        }

        @Override
        protected void writeResult(String name, Object value) throws IOException {
            if (failOnDuplicateResult && resultsByName.containsKey(name)) {
                fail("Result '" + name + "' already written");
            }
            resultsByName.put(name, value);
        }

    }
}
