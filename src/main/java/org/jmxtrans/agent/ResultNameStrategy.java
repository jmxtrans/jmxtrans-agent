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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.ObjectName;

import org.jmxtrans.agent.aliasing.DefaultQueryAliasFactory;
import org.jmxtrans.agent.aliasing.QueryAliasFactory;
import org.jmxtrans.agent.util.StringUtils2;

/**
 * Build a {@linkplain QueryResult#name} from a collected metric ({@linkplain Query}).
 * <p/>
 * Build name must be escaped to be compatible with all {@linkplain OutputWriter}.
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
public class ResultNameStrategy {

    private final Logger logger = Logger.getLogger(getClass().getName());
    /**
     * Function based evaluators for expressions like '#hostname#' or '#hostname_canonical#'
     */
    @Nonnull
    private final Map<String, Callable<String>> expressionEvaluators = new HashMap<String, Callable<String>>();
    @Nonnull
    private final QueryAliasFactory queryAliasFactory;
    
    public ResultNameStrategy() {
    	this(DefaultQueryAliasFactory.INSTANCE);
    }
    
    public ResultNameStrategy(QueryAliasFactory queryAliasFactory) {
    	this.queryAliasFactory = queryAliasFactory;
		registerExpressionEvaluators();
    }

	private void registerExpressionEvaluators() {
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
            logger.log(Level.SEVERE, "Exception resolving localhost, expressions like #hostname#, #canonical_hostname# or #hostaddress# will not be available", e);
        }
	}

    @Nonnull
    public String getResultName(@Nonnull Query query, @Nonnull ObjectName objectName) {
        return getResultName(query, objectName, null);
    }

    @Nonnull
    public String getResultName(@Nonnull Query query, @Nonnull ObjectName objectName, @Nullable String key) {
    	String resultAlias = query.getResultAlias();
        if (resultAlias == null || resultAlias.isEmpty()) {
        	return queryAliasFactory.valueOf(query, objectName, key);
        }
        return resolveExpression(query.getResultAlias(), objectName);
    }

    /**
     * Replace all the '#' based keywords (e.g. <code>#hostname#</code>) by their value.
     *
     * @param expression the expression to resolve (e.g. <code>"servers.#hostname#."</code>)
     * @return the resolved expression (e.g. <code>"servers.tomcat5"</code>)
     */
    @Nonnull
    public String resolveExpression(@Nonnull String expression) {
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
                        logger.log(Level.WARNING, "Error evaluating expression '" + key + "'", e);
                    }
                }
                StringUtils2.appendEscapedNonAlphaNumericChars(value, false, result);
                position = endingSeparatorPosition + 1;

            } else {
                result.append(c);
                position++;
            }
        }
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "resolveExpression(" + expression + "): " + result);
        return result.toString();

    }

    protected String resolveExpression(@Nonnull String expression, @Nonnull ObjectName exactObjectName) {

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
                StringUtils2.appendEscapedNonAlphaNumericChars(value, result);
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
                        logger.log(Level.WARNING, "Error evaluating expression '" + key + "'", e);
                    }
                }
                StringUtils2.appendEscapedNonAlphaNumericChars(value, false, result);
                position = endingSeparatorPosition + 1;

            } else {
                result.append(c);
                position++;
            }
        }
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "resolveExpression(" + expression + ", " + exactObjectName + "): " + result);

        return result.toString();
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
