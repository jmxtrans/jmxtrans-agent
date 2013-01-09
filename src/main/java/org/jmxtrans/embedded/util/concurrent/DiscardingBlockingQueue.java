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
package org.jmxtrans.embedded.util.concurrent;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automatically discard the oldest element if the queue is full.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class DiscardingBlockingQueue<E> extends ArrayBlockingQueue<E> implements DiscardingBlockingQueueMBean {

    private final AtomicInteger discardedElementCount = new AtomicInteger();

    /**
     * Creates a {@code DiscardingBlockingQueue} with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public DiscardingBlockingQueue(int capacity) {
        super(capacity);
    }

    /**
     * Creates a {@code DiscardingBlockingQueue} with the given (fixed)
     * capacity and the specified access policy.
     *
     * @param capacity the capacity of this queue
     * @param fair if {@code true} then queue accesses for threads blocked
     *        on insertion or removal, are processed in FIFO order;
     *        if {@code false} the access order is unspecified.
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public DiscardingBlockingQueue(int capacity, boolean fair) {
        super(capacity, fair);
    }

    /**
     * Creates an {@code DiscardingBlockingQueue} with the given (fixed)
     * capacity, the specified access policy and initially containing the
     * elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param capacity the capacity of this queue
     * @param fair if {@code true} then queue accesses for threads blocked
     *        on insertion or removal, are processed in FIFO order;
     *        if {@code false} the access order is unspecified.
     * @param c the collection of elements to initially contain
     * @throws IllegalArgumentException if {@code capacity} is less than
     *         {@code c.size()}, or less than 1.
     * @throws NullPointerException if the specified collection or any
     */
    public DiscardingBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
        super(capacity, fair, c);
    }

    /**
     * Offer the given element to the {@linkplain java.util.concurrent.BlockingQueue}
     * removing elements if necessary (ie if the queue is full).
     *
     * @param e the element to add to the queue
     */
    protected void discardingOffer(E e) {
        while (!super.offer(e)) {
            // remove elements as long as offer() fails.
            poll();
            discardedElementCount.incrementAndGet();
        }
    }

    /**
     * Add the given element to the {@linkplain java.util.concurrent.BlockingQueue}
     * removing elements if necessary (ie if the queue is full).
     *
     * @param e the element to add to the queue
     * @return <code>true</code>
     */
    @Override
    public boolean add(E e) {
        discardingOffer(e);
        return true;
    }

    /**
     * Offer the given element to the {@linkplain java.util.concurrent.BlockingQueue}
     * removing elements if necessary (ie if the queue is full).
     *
     * @param e the element to add to the queue
     * @return <code>true</code>
     */
    @Override
    public boolean offer(E e) {
        discardingOffer(e);
        return true;
    }

    /**
     * Add the given element to the {@linkplain java.util.concurrent.BlockingQueue}
     * removing elements if necessary (ie if the queue is full).
     *
     * @param e the element to add to the queue
     */
    @Override
    public void put(E e) throws InterruptedException {
        discardingOffer(e);
    }

    /**
     * Offer the given element to the {@linkplain java.util.concurrent.BlockingQueue}
     * removing elements if necessary (ie if the queue is full).
     *
     * @param e the element to add to the queue
     * @return <code>true</code>
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        discardingOffer(e);
        return true;
    }

    /**
     * Offer the given elements to the {@linkplain java.util.concurrent.BlockingQueue}
     * removing elements if necessary (ie if the queue is full).
     *
     * @param c the element to add to the queue
     * @return <code>true</code> if the given elements collection is not empty
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E e : c) {
            discardingOffer(e);
        }
        return !c.isEmpty();
    }

    @Override
    public int getDiscardedElementCount() {
        return discardedElementCount.get();
    }
}
