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
package org.jmxtrans.embedded.util;

import javax.annotation.Nullable;

/**
 * Inspired by {@link com.google.common.base.Preconditions}
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class Preconditions {
    private Preconditions() {
    }

    /**
     * Check the nullity of the given <code>reference</code>.
     *
     * @param reference reference to check
     * @return the given <code>reference</code>
     * @throws NullPointerException if the given <code>reference</code> is <code>null</code>
     */
    public static <T> T checkNotNull(T reference) throws NullPointerException {
        return checkNotNull(reference, null);
    }

    /**
     * Check the nullity of the given <code>reference</code>.
     *
     * @param reference reference to check
     * @param message   exception message, can be <code>null</code>
     * @return the given <code>reference</code>
     * @throws NullPointerException if the given <code>reference</code> is <code>null</code>
     */
    public static <T> T checkNotNull(T reference, @Nullable String message) throws NullPointerException {
        if (reference == null) {
            throw new NullPointerException(message == null ? "" : message);
        }
        return reference;
    }

    /**
     * Check the nullity and emptiness of the given <code>reference</code>.
     *
     * @param reference reference to check
     * @return the given <code>reference</code>
     * @throws IllegalArgumentException if the given <code>reference</code> is <code>null</code>
     */
    public static String checkNotEmpty(String reference) throws IllegalArgumentException {
        return checkNotEmpty(reference, null);
    }

    /**
     * Check the nullity and emptiness of the given <code>reference</code>.
     *
     * @param reference reference to check
     * @param message   exception message, can be <code>null</code>
     * @return the given <code>reference</code>
     * @throws IllegalArgumentException if the given <code>reference</code> is <code>null</code>
     */
    public static String checkNotEmpty(String reference, @Nullable String message) throws IllegalArgumentException {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException(message == null ? "Null or empty value" : message);
        }
        return reference;
    }
}
