/*
 * Copyright (c) 2015 the original author or authors
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

/**
 * Builds names with general rules like JConsole / VisualVM do.
 * i.e. <domain-name>.<property-name><property-name><attribute-name><composite-data-key-name>
 *
 * E.g. For objectName = "type:name=metric,value=bar" and attribute "count",
 *      it will general resultName = "type.metric.bar.count"
 *
 * @author <a href="mailto:maheshkelkar@gmail.com">Mahesh V Kelkar</a>
 */
public class JConsoleNameStrategyImpl implements ResultNameStrategy {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private ExpressionLanguageEngine expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @Nonnull
    @Override
    public String getResultName(@Nonnull Query query, @Nonnull ObjectName objectName, @Nullable String key, @Nonnull String attribute) {
        String result;
        if (query.getResultAlias() == null) {
            result = escapeObjectName(objectName);
            if (attribute != null && !attribute.isEmpty()) {
                result += "." + attribute;
            }
            if (key != null && !key.isEmpty()) {
                result += "." + key;
            }
        } else {
            result = expressionLanguageEngine.resolveExpression(query.getResultAlias(), objectName);
        }
        return result;
    }
    /**
     * Transforms an {@linkplain javax.management.ObjectName} into a plain {@linkplain String}
     * only composed of ('a' to 'Z', 'A' to 'Z', '.', '_') similar to JConsole naming
     *
     * '_' is the escape char for not compliant chars.
     */
    protected String escapeObjectName(@Nonnull ObjectName objectName) {

        /** Add objectName's domain */
        StringBuilder result = new StringBuilder();
        StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getDomain(), false, result);

        /** Walk through (sorted) properties of the ObjectName and add values to the result */
        List<String> keys = Collections.list(objectName.getKeyPropertyList().keys());
        Collections.sort(keys);
        for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String propertyKey = it.next();
            result.append('.');
            StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getKeyProperty(propertyKey), false,
                    result);
        }

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
