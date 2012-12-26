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

import javax.management.ObjectName;
import java.util.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ResultNameStrategy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
     * @param str    the string to escape
     * @param result the {@linkplain StringBuilder} in which the escaped string is appended
     */
    private void appendEscapedNonAlphaNumericChars(String str, StringBuilder result) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isLetter(ch) || Character.isDigit(ch)) {
                result.append(ch);
            } else if (ch == '"' && ((i == 0) || (i == chars.length - 1))) {
                // ignore starting and ending '"' that are used to quote() objectname's values (see ObjectName.value())
            } else {
                result.append('_');
            }
        }
    }
}
