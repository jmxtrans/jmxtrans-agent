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
public class ExpressionLanguageEngineTest {

    static ExpressionLanguageEngineImpl expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @BeforeClass
    public static void beforeClass() throws Exception {
        expressionLanguageEngine.registerExpressionEvaluator("hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1"));
        expressionLanguageEngine.registerExpressionEvaluator("canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1.www.private.mycompany.com"));
        expressionLanguageEngine.registerExpressionEvaluator("escaped_canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1_www_private_mycompany_com"));
        expressionLanguageEngine.registerExpressionEvaluator("hostaddress", new ExpressionLanguageEngineImpl.StaticFunction("10.0.0.81"));
        expressionLanguageEngine.registerExpressionEvaluator("escaped_hostaddress", new ExpressionLanguageEngineImpl.StaticFunction("10_0_0_81"));
    }

    /**
     * Test an expression with '#' based keywords (#hostname#) and with '%' based variables mapped to objectname properties.
     * @throws Exception
     */
    @Test
    public void test_resolve_simple_expression() throws Exception {
        // prepare
        String expression = "#hostname#.tomcat.datasource.%host%.%path%.%name%.#attribute#";
        String objectName = "Catalina:type=Resource,resourcetype=Context,path=/,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";

        // test
        String actual = expressionLanguageEngine.resolveExpression(expression, new ObjectName(objectName), "numActive", null, null);

        // verify
        assertThat(actual, is("tomcat1.tomcat.datasource.localhost._.jdbc_my-datasource.numActive"));
    }

    /**
     * Systems like Solr may include valuable information in the MXBean domain (ex: Solr core name). Test a function to
     * retrieve that domain.
     * @throws Exception
     */
    @Test
    public void test_resolve_expression_with_domain() throws Exception {
        // prepare
        String regular = "#hostname#.#domain#.%type%.%id%";
        String reversed = "#hostname#.#reversed_domain#.%type%.%id%";
        String objectName = "solr/demo.solr:type=/spell,id=Lazy[solr.SearchHandler]";

        // test
        String withRegularDomain = expressionLanguageEngine.resolveExpression(regular, new ObjectName(objectName), "numActive", null, null);
        String withReversedDomain = expressionLanguageEngine.resolveExpression(reversed, new ObjectName(objectName), "numActive", null, null);

        // verify
        assertThat(withRegularDomain, is("tomcat1.solr_demo.solr._spell.Lazy_solr_SearchHandler_"));
        assertThat(withReversedDomain, is("tomcat1.solr.solr_demo._spell.Lazy_solr_SearchHandler_"));
    }


    @Test
    public void test_resolve_expression_with_composite_data_key() throws Exception {
        // prepare
        String expression = "#hostname#.tomcat.jmv.gc.sp_mark_sweep.#key#";
        String objectName = "java.lang:type=GarbageCollector,name=PS MarkSweep";

        // test
        String actual = expressionLanguageEngine.resolveExpression(expression, new ObjectName(objectName), "LastGcInfo", "endTime", null);

        // verify
        assertThat(actual, is("tomcat1.tomcat.jmv.gc.sp_mark_sweep.endTime"));
    }

    @Test
    public void test_resolve_expression_with_position() throws Exception {
        // prepare
        String expression = "#hostname#.tomcat.mybean.#position#";
        String objectName = "mydomain:type=MyBean";

        // test
        String actual = expressionLanguageEngine.resolveExpression(expression, new ObjectName(objectName), "MyAttribute", null, 1);

        // verify
        assertThat(actual, is("tomcat1.tomcat.mybean.1"));
    }


    @Test
    public void test_canonical_hostname_dots_are_not_escaped() throws Exception {
        ExpressionLanguageEngineImpl engine = new ExpressionLanguageEngineImpl();
        engine.registerExpressionEvaluator("canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("server1.mycompany.com"));
        String actual = engine.resolveExpression("#canonical_hostname#");
        assertThat(actual, is("server1.mycompany.com"));
    }

    @Test
    public void test_escape_hashtag() throws Exception {
        ExpressionLanguageEngineImpl engine = new ExpressionLanguageEngineImpl();
        String actual = engine.resolveExpression("\\#tag=value");
        assertThat(actual, is("\\#tag=value"));
    }
}