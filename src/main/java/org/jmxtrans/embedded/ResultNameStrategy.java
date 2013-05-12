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

import org.jmxtrans.embedded.util.StringUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.ObjectName;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Build a {@linkplain QueryResult#name} from a collected metric ({@linkplain QueryAttribute}, {@linkplain Query}).
 * <p/>
 * Build name must be escaped to be compatible with all {@linkplain org.jmxtrans.embedded.output.OutputWriter}.
 * The approach is to escape non alpha-numeric chars.
 * <p/>
 * Expressions support '#' based keywords (e.g. <code>#hostname#</code>) and with '%' based variables mapped to objectname properties.
 * <p/>
 * Supported '#' based 'functions':
 * <table>
 * <tr>
 * <th>Function</th>
 * <th>Description</th>
 * <th>Sample</th>
 * </tr>
 * <tr>
 * <th><code>#hostname#</code></th>
 * <td>localhost - hostname {@link java.net.InetAddress#getHostName()}</td>
 * <td>TODO</td>
 * </tr>
 * <tr>
 * <th><code>#reversed_hostname#</code></th>
 * <td>reversed localhost - hostname {@link java.net.InetAddress#getHostName()}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <th><code>#escaped_hostname#</code></th>
 * <td>localhost - hostname {@link java.net.InetAddress#getHostName()} with '.' replaced by '_'</td>
 * <td>TODO</td>
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
 * <td>TODO</td>
 * </tr>
 * <tr>
 * <th><code>#escaped_hostname#</code></th>
 * <td>localhost - hostaddress {@link java.net.InetAddress#getHostAddress()} with '.' replaced by '_'</td>
 * <td>TODO</td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ResultNameStrategy {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Function based evaluators for expressions like '#hostname#' or '#hostname_canonical#'
     */
    @Nonnull
    private Map<String, Callable<String>> expressionEvaluators = new HashMap<String, Callable<String>>();

    public ResultNameStrategy() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String hostName = localHost.getHostName();
            String reversedHostName = StringUtils2.reverseTokens(hostName, ".");
            String canonicalHostName = localHost.getCanonicalHostName();
            String reversedCanonicalHostName = StringUtils2.reverseTokens(canonicalHostName, ".");
            String hostAddress = localHost.getHostAddress();

            registerExpressionEvaluator("hostname", hostName);
            registerExpressionEvaluator("reversed_hostname", reversedHostName);
            registerExpressionEvaluator("escaped_hostname", hostName.replaceAll("\\.", "_"));
            registerExpressionEvaluator("canonical_hostname", canonicalHostName);
            registerExpressionEvaluator("reversed_canonical_hostname", reversedCanonicalHostName);
            registerExpressionEvaluator("escaped_canonical_hostname", canonicalHostName.replaceAll("\\.", "_"));
            registerExpressionEvaluator("hostaddress", hostAddress);
            registerExpressionEvaluator("escaped_hostaddress", hostAddress.replaceAll("\\.", "_"));
        } catch (Exception e) {
            logger.error("Exception resolving localhost, expressions like #hostname#, #canonical_hostname# or #hostaddress# will not be available", e);
        }
    }

    public String getResultName(Query query, ObjectName objectName, QueryAttribute queryAttribute) {

        StringBuilder result = _getResultName(query, objectName, queryAttribute);

        return result.toString();
    }

    public String getResultName(Query query, ObjectName objectName, QueryAttribute queryAttribute, String key) {
        StringBuilder result = _getResultName(query, objectName, queryAttribute);
        result.append(".");
        result.append(key);
        return result.toString();
    }

    protected StringBuilder _getResultName(Query query, ObjectName objectName, QueryAttribute queryAttribute) {
        StringBuilder result = new StringBuilder();

        String queryName;
        if (query.getResultAlias() == null) {
            queryName = escapeObjectName(objectName);
        } else {
            queryName = resolveExpression(query.getResultAlias(), objectName);
        }

        if (queryName != null && !queryName.isEmpty()) {
            result.append(queryName).append(".");
        }

        String attributeName;
        if (queryAttribute.getResultAlias() == null) {
            attributeName = queryAttribute.getName();
        } else {
            attributeName = queryAttribute.getResultAlias();
        }
        result.append(attributeName);
        return result;
    }

    /**
     * Replace all the '#' based keywords (e.g. <code>#hostname#</code>) by their value.
     *
     * @param expression the expression to resolve (e.g. <code>"servers.#hostname#."</code>)
     * @return the resolved expression (e.g. <code>"servers.tomcat5"</code>)
     */
    public String resolveExpression(String expression) {
        StringBuilder result = new StringBuilder(expression.length());

        int position = 0;
        while (position < expression.length()) {
            char c = expression.charAt(position);
            if (c == '#') {
                int beginningSeparatorPosition = position;
                int endingSeparatorPosition = expression.indexOf('#', beginningSeparatorPosition + 1);
                if (endingSeparatorPosition == -1) {
                    throw new IllegalStateException("Invalid expression '" + expression + "', no ending '#' after beginning '#' at position " + beginningSeparatorPosition);
                }
                String key = expression.substring(beginningSeparatorPosition + 1, endingSeparatorPosition);
                Callable<String> expressionProcessor = expressionEvaluators.get(key);
                String value;
                if (expressionProcessor == null) {
                    value = "#unsupported_expression#";
                    logger.info("Unsupported expression '" + key + "'");
                } else {
                    try {
                        value = expressionProcessor.call();
                    } catch (Exception e) {
                        value = "#expression_error#";
                        logger.warn("Error evaluating expression '" + key + "'", e);
                    }
                }
                appendEscapedNonAlphaNumericChars(value, result);
                position = endingSeparatorPosition + 1;

            } else {
                result.append(c);
                position++;
            }
        }
        logger.trace("resolveExpression({}): {}", expression, result);
        return result.toString();

    }

    protected String resolveExpression(String expression, ObjectName exactObjectName) {

        StringBuilder result = new StringBuilder(expression.length());

        int position = 0;
        while (position < expression.length()) {
            char c = expression.charAt(position);
            if (c == '%') {
                int beginningSeparatorPosition = position;
                int endingSeparatorPosition = expression.indexOf('%', beginningSeparatorPosition + 1);
                if (endingSeparatorPosition == -1) {
                    throw new IllegalStateException("Invalid expression '" + expression + "', no ending '%' after beginning '%' at position " + beginningSeparatorPosition);
                }
                String key = expression.substring(beginningSeparatorPosition + 1, endingSeparatorPosition);
                String value = exactObjectName.getKeyProperty(key);
                if (value == null) {
                    value = "null";
                }
                appendEscapedNonAlphaNumericChars(value, result);
                position = endingSeparatorPosition + 1;
            } else if (c == '#') {
                int beginningSeparatorPosition = position;
                int endingSeparatorPosition = expression.indexOf('#', beginningSeparatorPosition + 1);
                if (endingSeparatorPosition == -1) {
                    throw new IllegalStateException("Invalid expression '" + expression + "', no ending '#' after beginning '#' at position " + beginningSeparatorPosition);
                }
                String key = expression.substring(beginningSeparatorPosition + 1, endingSeparatorPosition);
                Callable<String> expressionProcessor = expressionEvaluators.get(key);
                String value;
                if (expressionProcessor == null) {
                    value = "#unsupported_expression#";
                    logger.info("Unsupported expression '" + key + "'");
                } else {
                    try {
                        value = expressionProcessor.call();
                    } catch (Exception e) {
                        value = "#expression_error#";
                        logger.warn("Error evaluating expression '" + key + "'", e);
                    }
                }
                appendEscapedNonAlphaNumericChars(value, result);
                position = endingSeparatorPosition + 1;

            } else {
                result.append(c);
                position++;
            }
        }
        logger.trace("resolveExpression({}, {}): {}", expression, exactObjectName, result);
        return result.toString();
    }

    /**
     * Transforms an {@linkplain ObjectName} into a plain {@linkplain String} only composed of (a->Z, A-Z, '_').
     * <p/>
     * '_' is the escape char for not compliant chars.
     */
    protected String escapeObjectName(ObjectName objectName) {
        StringBuilder result = new StringBuilder();
        appendEscapedNonAlphaNumericChars(objectName.getDomain(), result);
        result.append('.');
        List<String> keys = Collections.list(objectName.getKeyPropertyList().keys());
        Collections.sort(keys);
        for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String key = it.next();
            appendEscapedNonAlphaNumericChars(key, result);
            result.append("__");
            appendEscapedNonAlphaNumericChars(objectName.getKeyProperty(key), result);
            if (it.hasNext()) {
                result.append('.');
            }
        }
        logger.trace("escapeObjectName({}): {}", objectName, result);
        return result.toString();
    }

    /**
     * Escape all non a->z,A->Z, 0->9 and '-' with a '_'.
     *
     * @param str    the string to escape
     * @param result the {@linkplain StringBuilder} in which the escaped string is appended
     */
    private void appendEscapedNonAlphaNumericChars(String str, StringBuilder result) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isLetter(ch) || Character.isDigit(ch) || ch == '-') {
                result.append(ch);
            } else if (ch == '"' && ((i == 0) || (i == chars.length - 1))) {
                // ignore starting and ending '"' that are used to quote() objectname's values (see ObjectName.value())
            } else {
                result.append('_');
            }
        }
    }

    /**
     * Registers an expression evaluator with a static value.
     */
    public void registerExpressionEvaluator(String expression, Callable<String> evaluator) {
        expressionEvaluators.put(expression, evaluator);
    }

    /**
     * Registers an expression evaluator with a static value.
     */
    public void registerExpressionEvaluator(String expression, String value) {
        expressionEvaluators.put(expression, new StaticEvaluator(value));
    }

    @Nonnull
    public Map<String, Callable<String>> getExpressionEvaluators() {
        return expressionEvaluators;
    }

    public static class StaticEvaluator implements Callable<String> {
        final String value;

        public StaticEvaluator(String value) {
            this.value = value;
        }

        @Override
        public String call() throws Exception {
            return value;
        }

        @Override
        public String toString() {
            return "StaticStringCallable{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }
}
