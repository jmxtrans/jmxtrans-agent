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
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class ExpressionLanguageEngineImpl implements ExpressionLanguageEngine {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    public ExpressionLanguageEngineImpl() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String hostName = localHost.getHostName();
            String reversedHostName = StringUtils2.reverseTokens(hostName, ".");
            String canonicalHostName = localHost.getCanonicalHostName();
            String reversedCanonicalHostName = StringUtils2.reverseTokens(canonicalHostName, ".");
            String hostAddress = localHost.getHostAddress();

            functionsByName.put("hostname", new StaticFunction(hostName));
            functionsByName.put("reversed_hostname", new StaticFunction(reversedHostName));
            functionsByName.put("escaped_hostname", new StaticFunction(hostName.replaceAll("\\.", "_")));
            functionsByName.put("canonical_hostname", new StaticFunction(canonicalHostName));
            functionsByName.put("reversed_canonical_hostname", new StaticFunction(reversedCanonicalHostName));
            functionsByName.put("escaped_canonical_hostname", new StaticFunction(canonicalHostName.replaceAll("\\.", "_")));
            functionsByName.put("hostaddress", new StaticFunction(hostAddress));
            functionsByName.put("escaped_hostaddress", new StaticFunction(hostAddress.replaceAll("\\.", "_")));
            functionsByName.put("attribute", new ExtractAttributeFunction());
            functionsByName.put("key", new ExtractCompositeDataKeyFunction());
            functionsByName.put("compositeDataKey", new ExtractCompositeDataKeyFunction());
            functionsByName.put("position", new ExtractPositionFunction());
            functionsByName.put("domain", new ExtractDomainFunction());
            functionsByName.put("reversed_domain", new ExtractReversedDomainFunction());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception resolving localhost, expressions like #hostname#, #canonical_hostname# or #hostaddress# will not be available", e);
        }
    }

    /**
     * Function based evaluators for expressions like '#hostname#' or '#hostname_canonical#'
     */
    @Nonnull
    private Map<String, Function> functionsByName = new HashMap<>();

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
                Function expressionProcessor = functionsByName.get(key);
                String value;
                if (expressionProcessor == null) {
                    value = "#unsupported_expression#";
                    logger.info("Unsupported expression '" + key + "'");
                } else {
                    try {
                        value = expressionProcessor.evaluate();
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

    @Nonnull
    public String resolveExpression(@Nonnull String expression, @Nonnull ObjectName exactObjectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {

        StringBuilder result = new StringBuilder(expression.length());

        int pos = 0;
        while (pos < expression.length()) {
            char c = expression.charAt(pos);
            if (c == '%') {
                int beginningSeparatorPosition = pos;
                int endingSeparatorPosition = expression.indexOf('%', beginningSeparatorPosition + 1);
                if (endingSeparatorPosition == -1) {
                    throw new IllegalStateException("Invalid expression '" + expression + "', no ending '%' after beginning '%' at position " + beginningSeparatorPosition);
                }
                String objectNameKey = expression.substring(beginningSeparatorPosition + 1, endingSeparatorPosition);
                String value = exactObjectName.getKeyProperty(objectNameKey);
                if (value == null) {
                    value = "null";
                }
                StringUtils2.appendEscapedNonAlphaNumericChars(value, result);
                pos = endingSeparatorPosition + 1;
            } else if (c == '#') {
                int beginningSeparatorPosition = pos;
                int endingSeparatorPosition = expression.indexOf('#', beginningSeparatorPosition + 1);
                if (endingSeparatorPosition == -1) {
                    throw new IllegalStateException("Invalid expression '" + expression + "', no ending '#' after beginning '#' at position " + beginningSeparatorPosition);
                }
                String functionName = expression.substring(beginningSeparatorPosition + 1, endingSeparatorPosition);
                Function function = functionsByName.get(functionName);
                String value;
                if (function == null) {
                    value = "#unsupported_expression#";
                    logger.info("Unsupported expression '" + functionName + "'");
                } else {
                    try {
                        value = function.evaluate(exactObjectName, attribute, compositeDataKey, position);
                    } catch (Exception e) {
                        value = "#expression_error#";
                        logger.log(Level.WARNING, "Error evaluating expression '" + compositeDataKey + "'", e);
                    }
                }
                StringUtils2.appendEscapedNonAlphaNumericChars(value, false, result);
                pos = endingSeparatorPosition + 1;

            } else {
                result.append(c);
                pos++;
            }
        }
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "resolveExpression(" + expression + ", " + exactObjectName + "): " + result);

        return result.toString();
    }

    /**
     * Registers an expression evaluator with a static value.
     */
    public void registerExpressionEvaluator(@Nonnull String expression, @Nonnull Function evaluator) {
        functionsByName.put(expression, evaluator);
    }

    public interface Function {
        @Nullable
        String evaluate();
        @Nullable
        String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position);
    }

    public static class StaticFunction implements Function {
        final String value;

        public StaticFunction(String value) {
            this.value = value;
        }

        @Override
        public String evaluate() {
            return value;
        }

        @Override
        public String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
            return value;
        }

        @Override
        public String toString() {
            return "StaticFunction{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }

    private static class ExtractAttributeFunction implements Function {
        @Override
        public String evaluate() {
            return null;
        }

        @Override
        public String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
            return attribute;
        }
    }

    private static class ExtractCompositeDataKeyFunction implements Function {
        @Override
        public String evaluate() {
            return null;
        }

        @Override
        public String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
            return compositeDataKey;
        }
    }

    private static class ExtractPositionFunction implements Function {
        @Override
        public String evaluate() {
            return null;
        }

        @Override
        public String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
            return position == null ? null : position.toString();
        }
    }

    private static class ExtractDomainFunction implements Function {
        @Override
        public String evaluate() {
            return null;
        }

        @Override
        public String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
            return objectName == null ? null : objectName.getDomain();
        }
    }

    private static class ExtractReversedDomainFunction implements Function {
        @Override
        public String evaluate() {
            return null;
        }

        @Override
        public String evaluate(@Nullable ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
            return objectName == null ? null : StringUtils2.reverseTokens(objectName.getDomain(), ".");
        }
    }
}
