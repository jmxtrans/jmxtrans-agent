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
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ResultNameStrategyTest {

    static ResultNameStrategyImpl strategy = new ResultNameStrategyImpl();

    @BeforeClass
    public static void beforeClass() throws Exception {
        ExpressionLanguageEngineImpl expressionLanguageEngine = new ExpressionLanguageEngineImpl();
        strategy.setExpressionLanguageEngine(expressionLanguageEngine);
        expressionLanguageEngine.registerExpressionEvaluator("hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1"));
        expressionLanguageEngine.registerExpressionEvaluator("canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1.www.private.mycompany.com"));
        expressionLanguageEngine.registerExpressionEvaluator("escaped_canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1_www_private_mycompany_com"));
        expressionLanguageEngine.registerExpressionEvaluator("hostaddress", new ExpressionLanguageEngineImpl.StaticFunction("10.0.0.81"));
        expressionLanguageEngine.registerExpressionEvaluator("escaped_hostaddress", new ExpressionLanguageEngineImpl.StaticFunction("10_0_0_81"));
    }

    @Test
    public void testEscapeObjectName1() throws Exception {
        String objectName = "java.lang:type=GarbageCollector,name=PS Scavenge";
        String actual = strategy.escapeObjectName(new ObjectName(objectName));
        assertThat(actual, is("java_lang.name__PS_Scavenge.type__GarbageCollector"));
    }

    @Test
    public void testEscapeObjectName2() throws Exception {
        String objectName = "Catalina:type=Resource,resourcetype=Context,path=/,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.escapeObjectName(new ObjectName(objectName));
        assertThat(actual, is("Catalina.class__javax_sql_DataSource.host__localhost.name__jdbc_my-datasource.path___.resourcetype__Context.type__Resource"));
    }
}
