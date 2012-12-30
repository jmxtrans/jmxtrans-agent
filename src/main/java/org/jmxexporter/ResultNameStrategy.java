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
 * Build name must be escaped to be compatible with all {@linkplain org.jmxexporter.output.OutputWriter}.
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
 * <th><code>#escaped_hostname#</code></th>
 * <td>localhost - hostname {@link java.net.InetAddress#getHostName()} with '.' replaced by '_'</td>
 * <td>TODO</td>
 * </tr>
 * <tr>
 * <th><code>#canonical_hostname#</code></th>
 * <td>localhost - canonical hostname {@link java.net.InetAddress#getCanonicalHostName()}</td>
 * <td>TODO</td>
 * </tr>
 * <tr>
 * <th><code>#escaped_canonical_hostname#</code></th>
 * <td>localhost - canonical hostname {@link java.net.InetAddress#getCanonicalHostName()} with '.' replaced by '_'</td>
 * <td>TODO</td>
 * </tr>
 * <tr>
 * <th><code>#hostaddress#</code></th>
 * <td>localhost - hostaddress {@link java.net.InetAddress#getHostAddress()}</td>
 * </tr>
 * <tr>
 * <th><code>#escaped_hostname#</code></th>
 * <td>localhost - hostaddress {@link java.net.InetAddress#getHostAddress()} with '.' replaced by '_'</td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ResultNameStrategy {

    public static class StaticEvaluator implements Callable<String> {
        public StaticEvaluator(String value) {
            this.value = value;
        }

        final String value;

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
            String canonicalHostName = localHost.getCanonicalHostName();
            String hostAddress = localHost.getHostAddress();

            registerExpressionEvaluator("hostname", hostName);
            registerExpressionEvaluator("escaped_hostname", hostName.replaceAll("\\.", "_"));
            registerExpressionEvaluator("canonical_hostname", canonicalHostName);
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

        if (query.getResultAlias() == null) {
            result.append(escapeObjectName(objectName));
        } else {
            result.append(resolveExpression(query.getResultAlias(), objectName));
        }

        result.append(".");
        if (queryAttribute.getResultAlias() == null) {
            result.append(queryAttribute.getName());
        } else {
            result.append(queryAttribute.getResultAlias());
        }
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
}
