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

import javax.annotation.Nullable;

/**
 * Inspired by <code>com.google.common.base.Preconditions</code>
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Preconditions2 {
    public static <T> T checkNotNull(T t) {
        if (t == null)
            throw new NullPointerException();
        return t;
    }

    public static String checkNotEmpty(String str) {
        if (str == null)
            throw new NullPointerException();
        if (str.isEmpty())
            throw new IllegalArgumentException("Can not be empty");
        return str;
    }

    public static void checkArgument(boolean expression) throws IllegalArgumentException {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, @Nullable String msg) throws IllegalArgumentException {
        if (!expression) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * @param expression
     * @param msgFormat
     * @param msgArgs
     * @throws IllegalArgumentException
     * @see String#format(String, Object...)
     */
    public static void checkArgument(boolean expression, @Nullable String msgFormat, Object... msgArgs) throws IllegalArgumentException {
        if (!expression) {
            if (msgFormat == null) {
                throw new IllegalArgumentException();
            } else {
                throw new IllegalArgumentException(String.format(msgFormat, msgArgs));
            }
        }
    }
}
