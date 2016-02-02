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
package org.jmxtrans.agent.util.collect;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


/**
 * Inspired by <code>com.google.common.collect.Iterables</code>.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Iterables2 {

    private Iterables2(){}
    
    /**
     * Returns the element at the specified position in an iterable.
     *
     * @param iterable the iterable to search into
     * @param position the position of the entry to return
     * @return the entry at the given <code>location</code> in the given <code>iterable</code>
     *         throws
     * @throws IndexOutOfBoundsException if given {@code position} is negative or
     *                                   greater than or equal to the size of given <code>iterable</code>
     */
    @Nullable
    public static <T> T get(@Nonnull Iterable<T> iterable, @Nonnegative int position) throws NullPointerException, IndexOutOfBoundsException {
        if (iterable == null)
            throw new NullPointerException("iterable");

        if (iterable instanceof List) {
            return ((List<T>) iterable).get(position);
        }

        if (position < 0)
            throw new IndexOutOfBoundsException("Requested position must be greater than 0, '" + position + "' is invalid");

        int idx = 0;
        for (T value : iterable) {
            if (idx == position) {
                return value;
            }
            idx++;
        }
        throw new IndexOutOfBoundsException("Requested position must be smaller than iterable size (" + idx + "), '" + position + "' is invalid");
    }
}
