/*
 * Copyright (c) 2010-2014 the original author or authors
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
package org.jmxtrans.agent.aliasing;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.management.ObjectName;

import org.jmxtrans.agent.Query;
import org.jmxtrans.agent.util.StringUtils2;

public enum DefaultQueryAliasFactory implements QueryAliasFactory {
    /* */
    INSTANCE;

    private final Logger logger = Logger.getLogger(getClass().getName());

    public String valueOf(Query query, ObjectName objectName, String key) {
        if (key == null) {
            return escapeObjectName(objectName) + "." + query.getAttribute();
        }
        return escapeObjectName(objectName) + "." + query.getAttribute() + "." + key;
    }

    /**
     * Transforms an {@linkplain javax.management.ObjectName} into a plain
     * {@linkplain String} only composed of (a->Z, A-Z, '_').
     * <p/>
     * '_' is the escape char for not compliant chars.
     */
    private String escapeObjectName(@Nonnull ObjectName objectName) {
        StringBuilder result = new StringBuilder();
        StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getDomain(), result);
        result.append('.');
        List<String> keys = Collections.list(objectName.getKeyPropertyList().keys());
        Collections.sort(keys);
        for (Iterator<String> it = keys.iterator(); it.hasNext();) {
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

    @Override
    public void postConstruct(Map<String, String> settings) {

    }
}
