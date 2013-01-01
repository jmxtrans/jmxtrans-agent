/*
 * Copyright 1012-2013 the original author or authors.
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
