/*
 * Copyright 2010-2013, the original author or authors
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
package org.jmxtrans.agent.util.collect;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class EvictingQueue<E> extends ForwardingQueue<E> implements Queue<E> {

    private LinkedBlockingQueue delegate;
    private int maxRetry = 10;

    public EvictingQueue(int capacity) {
        delegate = new LinkedBlockingQueue(capacity);
    }

    public static <E> EvictingQueue<E> create(int maxCapacity) {
        return new EvictingQueue<E>(maxCapacity);
    }

    @Nonnull
    @Override
    protected Queue<E> delegate() {
        return delegate;
    }

    @Override
    public boolean add(E e) {
        return addEvictingIfNeeded(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return standardAddAll(c);
    }

    @Override
    public boolean offer(E e) {
        return addEvictingIfNeeded(e);
    }

    protected boolean addEvictingIfNeeded(E e) {

        for (int i = 0; i < maxRetry; i++) {
            boolean offered = delegate().offer(e);
            if (offered) {
                return true;
            }
            delegate().poll();
        }

        return false;
    }
}
