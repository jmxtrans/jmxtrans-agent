/*
 * Copyright 2010-2013, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmxtrans.agent.util;

import java.io.Serializable;

/**
 * Inspired by Spring Property placeholder mechanism.
 * <p/>
 * Placeholders are delimited by '{' and '}' and support default value with ':'. Sample : '<code>{graphite.host}</code>'
 * or '<code>{graphite.port:2003}</code>'.
 * <p/>
 * If the placeholder is not found in the system properties, it is searched in the environment variables and then
 * converted to underscore delimited upper case and searched in environment variables.
 * <p/>
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

        return result.toString();
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
