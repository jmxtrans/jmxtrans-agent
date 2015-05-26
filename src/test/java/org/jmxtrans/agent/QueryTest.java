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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
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
    ResultNameStrategy resultNameStrategy = new ResultNameStrategyImpl();

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
        Query query = new Query("test:type=Mock,name=mock", "CollectionUsageThreshold", resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void basic_attribute_null_result_alias_returns_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "CollectionUsageThreshold", null, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("test.name__mock.type__Mock.CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void expression_language_substitutes_object_name_key() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "CollectionUsageThreshold", "test_%type%_%name%.CollectionUsageThreshold", resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("test_Mock_mock.CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void expression_language_substitutes_function() throws Exception {
        ExpressionLanguageEngineImpl engine = new ExpressionLanguageEngineImpl();
        engine.registerExpressionEvaluator("hostname", "my-hostname");
        ResultNameStrategyImpl resultNameStrategy = new ResultNameStrategyImpl();
        resultNameStrategy.setExpressionLanguageEngine(engine);
        Query query = new Query("test:type=Mock,name=mock", "CollectionUsageThreshold", "#hostname#.mock.CollectionUsageThreshold", resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("my-hostname.mock.CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void indexed_list_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntegerList", 1, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("IntegerList");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void non_indexed_list_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntegerList", resultNameStrategy);
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
        Query query = new Query("test:type=Mock,name=mock", "IntArray", 1, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("IntArray");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void indexed_integer_array_attribute_return_simple_result() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", "IntegerArray", 1, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("IntegerArray");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void query_wildcard_objectname_domain_returns_objetname_and_attribute() throws Exception {
        Query query = new Query("*:type=Mock,name=mock", "CollectionUsageThreshold", null, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("test.name__mock.type__Mock.CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void query_wildcard_objectname_property_returns_objetname_and_attribute() throws Exception {
        Query query = new Query("test:*", "CollectionUsageThreshold", null, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("test.name__mock.type__Mock.CollectionUsageThreshold");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void query_wildcard_objectname_domain_returns_meabn_with_resultalias() throws Exception {
        Query query = new Query("*:type=Mock,name=mock", "CollectionUsageThreshold", "altTest.%name%.%type%", resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("altTest.mock.Mock");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void query_wildcard_objectname_property_returns_mbean_with_resultalias() throws Exception {
        Query query = new Query("test:*", "CollectionUsageThreshold", "altTest.%name%.%type%", resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Object actual = mockOutputWriter.resultsByName.get("altTest.mock.Mock");
        assertThat(actual, notNullValue());
        assertThat(actual, instanceOf(Number.class));
    }

    @Test
    public void query_objectname_with_null_attribute_returns_all_attributes() throws Exception {
        Query query = new Query("test:type=Mock,name=mock", null, null, resultNameStrategy);
        query.collectAndExport(mbeanServer, mockOutputWriter);
        Integer actualSize = mockOutputWriter.resultsByName.size();
        assert(actualSize == 24);
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
        public void writeQueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value) throws IOException {
            if (failOnDuplicateResult && resultsByName.containsKey(name)) {
                fail("Result '" + name + "' already written");
            }
            resultsByName.put(name, value);
        }

        @Override
        public void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException {
            writeQueryResult(invocationName, null, value);
        }
    }
}
