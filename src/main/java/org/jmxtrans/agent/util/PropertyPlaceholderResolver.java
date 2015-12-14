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

import org.jmxtrans.agent.ExpressionLanguageEngine;
import org.jmxtrans.agent.ExpressionLanguageEngineImpl;

import java.io.Serializable;

/**
 * Inspired by Spring Property placeholder mechanism.
 *
 * Placeholders are delimited by '{' and '}' and support default value with ':'. Sample : '<code>{graphite.host}</code>'
 * or '<code>{graphite.port:2003}</code>'.
 *
 * If the placeholder is not found in the system properties, it is searched in the environment variables and then
 * converted to underscore delimited upper case and searched in environment variables.
 *
 * Sample for '<code>{graphite.port:2003}</code>':
 * <ol>
 * <li><code>System.getProperty("graphite.port")</code></li>
 * <li><code>System.getenv("graphite.port")</code></li>
 * <li><code>System.getenv("GRAPHITE_PORT")</code></li>
 * <li>default to <code>2003</code></li>
 * </ol>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class PropertyPlaceholderResolver implements Serializable {

    private static final long serialVersionUID = 1L;
    private ExpressionLanguageEngine expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    /**
     * Parse the given <code>string</code> resolving property placeholders (<code>${my-property[:default-value]}</code>)
     *
     * @param string the string to parse.
     * @return the parsed string. Non <code>null</code>.
     * @throws IllegalStateException a property placeholder could not be resolved and no default value has been defined.
     */
    public String resolveString(String string) throws IllegalStateException {

        StringBuilder result = new StringBuilder(string.length());

        int position = 0;
        while (position < string.length()) {
            char c = string.charAt(position);
            if (c == '$' && (position < string.length() - 1) && string.charAt(position + 1) == '{') {
                int beginningBracketPosition = position + 1;
                int endingBracketPosition = string.indexOf('}', beginningBracketPosition);
                int colonPosition = string.indexOf(':', beginningBracketPosition);
                if (colonPosition > endingBracketPosition) {
                    colonPosition = -1;
                }

                String placeholder;
                String defaultValue;
                if (colonPosition == -1) {
                    placeholder = string.substring(beginningBracketPosition + 1, endingBracketPosition);
                    defaultValue = null;
                } else {
                    placeholder = string.substring(beginningBracketPosition + 1, colonPosition);
                    defaultValue = string.substring(colonPosition + 1, endingBracketPosition);
                }

                String resolvedPlaceHolder = resolvePlaceholder(placeholder, defaultValue);
                result.append(resolvedPlaceHolder);
                position = endingBracketPosition + 1;
            } else {
                result.append(c);
                position++;
            }
        }
        return expressionLanguageEngine.resolveExpression(result.toString());
    }

    /**
     * Search for the given placeholder in system properties then in environment variables.
     *
     * @param property     property to resolve
     * @param defaultValue Default value if the placeholder is not found. <code>null</code> means not default value is
     *                     defined and the placeholder must exist
     * @return the resolved property or the default value if the placeholder is not found and the default value is defined. Non null.
     * @throws IllegalStateException if the placeholder is not found and the given <code>defaultValue</code> is not
     *                              defined (<code>null</code>)
     */
    protected String resolvePlaceholder(String property, String defaultValue) throws IllegalStateException {


        // "graphite.host" -> "GRAPHITE_HOST"
        String environmentVariableStyleProperty = property.toUpperCase();
        environmentVariableStyleProperty = environmentVariableStyleProperty.replaceAll("\\.", "_");


        String result;
        if (System.getProperties().containsKey(property)) {
            result = System.getProperty(property);
        } else if (System.getenv().containsKey(property)) {
            result = System.getenv(property);
        } else if (System.getenv().containsKey(environmentVariableStyleProperty)) {
            result = System.getenv(environmentVariableStyleProperty);
        } else if (defaultValue != null) {
            result = defaultValue;
        } else {
            throw new IllegalStateException("Property '" + property + "' not found in System properties nor in Environment variables");
        }
        return result;
    }

}
