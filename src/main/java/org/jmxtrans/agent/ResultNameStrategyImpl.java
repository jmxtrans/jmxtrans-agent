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

import org.jmxtrans.agent.util.StringUtils2;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.ObjectName;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Build a {@linkplain org.jmxtrans.agent.QueryResult#name} from a collected metric ({@linkplain org.jmxtrans.agent.Query}).
 *
 * Build name must be escaped to be compatible with all {@linkplain org.jmxtrans.agent.OutputWriter}.
 * The approach is to escape non alpha-numeric chars.
 *
 * Expressions support '#' based keywords (e.g. <code>#hostname#</code>) and with '%' based variables mapped to objectname properties.
 *
 * Supported '#' based 'functions':
 * <table summary="Functions">
 * <tr>
 * <th>Function</th>
 * <th>Description</th>
 * <th>Sample</th>
 * </tr>
 * <tr>
 * <th><code>#hostname#</code></th>
 * <td>localhost - hostname {@link java.net.InetAddress#getHostName()}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <th><code>#reversed_hostname#</code></th>
 * <td>reversed localhost - hostname {@link java.net.InetAddress#getHostName()}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <th><code>#escaped_hostname#</code></th>
 * <td>localhost - hostname {@link java.net.InetAddress#getHostName()} with '.' replaced by '_'</td>
 * <td></td>
 * </tr>
 * <tr>
 * <th><code>#canonical_hostname#</code></th>
 * <td>localhost - canonical hostname {@link java.net.InetAddress#getCanonicalHostName()}</td>
 * <td><code>server1.ecommerce.mycompany.com</code></td>
 * </tr>
 * <tr>
 * <th><code>#reversed_canonical_hostname#</code></th>
 * <td>reversed localhost - canonical hostname {@link java.net.InetAddress#getCanonicalHostName()}</td>
 * <td><code>com.mycompany.ecommerce.server1</code></td>
 * </tr>
 * <tr>
 * <th><code>#escaped_canonical_hostname#</code></th>
 * <td>localhost - canonical hostname {@link java.net.InetAddress#getCanonicalHostName()} with '.' replaced by '_'</td>
 * <td><code>server1_ecommerce_mycompany_com</code></td>
 * </tr>
 * <tr>
 * <th><code>#hostaddress#</code></th>
 * <td>localhost - hostaddress {@link java.net.InetAddress#getHostAddress()}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <th><code>#escaped_hostname#</code></th>
 * <td>localhost - hostaddress {@link java.net.InetAddress#getHostAddress()} with '.' replaced by '_'</td>
 * <td></td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class ResultNameStrategyImpl implements ResultNameStrategy {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    private ExpressionLanguageEngine expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @Nonnull
    @Override
    public String getResultName(@Nonnull Query query, @Nonnull ObjectName objectName, @Nullable String key, @Nonnull String attribute) {
        String result;
        if (query.getResultAlias() == null) {
            if (key == null) {
                result = escapeObjectName(objectName) + "." + attribute;
            } else {
                result = escapeObjectName(objectName) + "." + attribute + "." + key;
            }
        } else {
            result = expressionLanguageEngine.resolveExpression(query.getResultAlias(), objectName);
        }
        return result;
    }

    /**
     * Transforms an {@linkplain javax.management.ObjectName} into a plain {@linkplain String} only composed of ('a' to 'Z', 'A' to 'Z', '_').
     *
     * '_' is the escape char for not compliant chars.
     */
    protected String escapeObjectName(@Nonnull ObjectName objectName) {
        StringBuilder result = new StringBuilder();
        StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getDomain(), result);
        result.append('.');
        List<String> keys = Collections.list(objectName.getKeyPropertyList().keys());
        Collections.sort(keys);
        for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String key = it.next();
            StringUtils2.appendEscapedNonAlphaNumericChars(key, result);
            result.append("__");
            StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getKeyProperty(key), result);
            if (it.hasNext()) {
                result.append('.');
            }
        }
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "escapeObjectName(" + objectName + "): " + result);
        return result.toString();
    }

    public ExpressionLanguageEngine getExpressionLanguageEngine() {
        return expressionLanguageEngine;
    }

    public void setExpressionLanguageEngine(ExpressionLanguageEngine expressionLanguageEngine) {
        this.expressionLanguageEngine = expressionLanguageEngine;
    }

    public void postConstruct(@Nonnull Map<String, String> settings) {

    }
}
