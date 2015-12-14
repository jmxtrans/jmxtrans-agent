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
package org.jmxtrans.agent.util;

import org.jmxtrans.agent.ExpressionLanguageEngineImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class PropertyPlaceholderResolverTest {

    private PropertyPlaceholderResolver resolver = new PropertyPlaceholderResolver();
    static ExpressionLanguageEngineImpl expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @BeforeClass
    public static void beforeClass() throws Exception {
        expressionLanguageEngine.registerExpressionEvaluator("hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1"));
        expressionLanguageEngine.registerExpressionEvaluator("canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1.www.private.mycompany.com"));
        expressionLanguageEngine.registerExpressionEvaluator("escaped_canonical_hostname", new ExpressionLanguageEngineImpl.StaticFunction("tomcat1_www_private_mycompany_com"));
        expressionLanguageEngine.registerExpressionEvaluator("hostaddress", new ExpressionLanguageEngineImpl.StaticFunction("10.0.0.81"));
        expressionLanguageEngine.registerExpressionEvaluator("escaped_hostaddress", new ExpressionLanguageEngineImpl.StaticFunction("10_0_0_81"));
    }

    @Test
    public void testResolveStringWithSystemProperty() {
        System.setProperty("graphite.host", "graphite-server.private.mycompany.com");
        try {
            String actual = resolver.resolveString("${graphite.host:localhost}");
            assertThat(actual, is("graphite-server.private.mycompany.com"));
        } finally {
            System.getProperties().remove("graphite.host");
        }
    }

    @Test
    public void testResolveComplexStringWithSystemProperty() {
        System.setProperty("graphite.host", "graphite-server.private.mycompany.com");
        try {
            String actual = resolver.resolveString("${graphite.host:localhost}:${graphite.port:2003}");
            assertThat(actual, is("graphite-server.private.mycompany.com:2003"));
        } finally {
            System.getProperties().remove("graphite.host");
        }
    }

    @Test
    public void testResolveStringWithDefaultValue() {
        String actual = resolver.resolveString("${graphite.host:localhost}");
        assertThat(actual, is("localhost"));
    }

    @Test
    public void testResolveStringWithDefaultValueAndExpression() {
        String actual = resolver.resolveString("${graphite.host:localhost}.#hostname#");
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String hostName = localHost.getHostName();
            assertThat(actual, is("localhost." + hostName));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
