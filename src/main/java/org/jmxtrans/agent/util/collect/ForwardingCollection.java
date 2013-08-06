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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public abstract class ForwardingCollection<E> implements Collection<E> {

    @Nonnull
    protected abstract Collection<E> delegate();

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate().iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate().toArray(a);
    }

    public boolean add(E e) {
        return delegate().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return delegate().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate().containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return delegate().addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate().retainAll(c);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    protected boolean standardAddAll(Collection<? extends E> collection) {
        boolean result = true;
        for (E e : collection) {
            boolean added = add(e);
            result = result && added;
        }
        return result;
    }
}
