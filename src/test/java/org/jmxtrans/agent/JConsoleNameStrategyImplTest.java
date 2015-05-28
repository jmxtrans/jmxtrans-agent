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

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:maheshkelkar@gmail.com">Mahesh V Kelkar</a>
 */
public class JConsoleNameStrategyImplTest {
    static JConsoleNameStrategyImpl strategy = new JConsoleNameStrategyImpl();
    static ExpressionLanguageEngineImpl expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @BeforeClass
    public static void beforeClass() throws Exception {
        strategy.setExpressionLanguageEngine(expressionLanguageEngine);
    }

    @Test
    public void testGetResultName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), "usage", "count");
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource.count.usage"));
    }

    @Test
    public void testGetResultNameWithObjectName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), null, null);
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource"));
    }

    @Test
    public void testGetResultNameWithNullAttributeName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), "usage", null);
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource.usage"));
    }

    @Test
    public void testGetResultNameWithNullKeyName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), null, "count");
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource.count"));
    }

    @Test
    public void testGetResultNameWithResultAlias() throws Exception {
        Query query = new Query("Catalina:*", "count", "Katalina:%name%.%type%", strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), "usage", "count");
        assertThat(actual, is("Katalina:jdbc_my-datasource.Resource"));
    }
}
