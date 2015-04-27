/*
 * Copyright (c) 2010-2015 the original author or authors
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Maintains a cached version of the {@code Object} that it holds and handle the renewal of this object upon expiration.
 *
 * Greatly inspired by the {@code CachedData} sample provided in the javadoc
 * of {@link ReentrantReadWriteLock}.
 *
 * {@code Object} is created implementing the {@link #newObject()} method.
 *
 * Sample to get an {@code InetAddress} refreshed against a DNS every 10 seconds:
 * <pre><code>
 * CachingReference myRemoteServerAddress = new CachingReference&lt;InetAddress&gt;(10, TimeUnit.SECONDS) {
 *     protected InetAddress newObject() {
 *         try {
 *             return InetAddress.getByName(myRemoteServerHostname);
 *         } catch () {
 *             throw new RuntimeException("Exception resolving '" + myRemoteServerHostname + "'", e);
 *         }
 *     }
 * }
 * </code></pre>
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public abstract class CachingReference<E> {
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private long lastCreationInNanos;
    private long timeToLiveInNanos;
    private E object;

    public CachingReference(long timeToLiveInNanos) {
        this.timeToLiveInNanos = timeToLiveInNanos;
    }

    public CachingReference(long timeToLive, TimeUnit timeToLiveUnit) {
        this(TimeUnit.NANOSECONDS.convert(timeToLive, timeToLiveUnit));
    }

    /**
     * @return the newly created object.
     */
    @Nullable
    protected abstract E newObject();

    /**
     * @return the up to date version of the {@code Object} hold by this reference.
     */
    @Nullable
    public E get() {
        rwl.readLock().lock();
        try {
            if (object == null || ((System.nanoTime() - lastCreationInNanos) > timeToLiveInNanos)) {
                // Must release read lock before acquiring write lock
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                try {
                    // Recheck state because another thread might have
                    // acquired write lock and changed state before we did.
                    if (object == null || ((System.nanoTime() - lastCreationInNanos) > timeToLiveInNanos)) {
                        object = newObject();
                        lastCreationInNanos = System.nanoTime();
                    }
                } finally {
                    // Downgrade by acquiring read lock before releasing write lock
                    rwl.readLock().lock();
                    rwl.writeLock().unlock();
                }
            }
            return object;
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Purge the cached reference so that a new object will be created for the next {@link #get()}
     */
    public void purge() {
        rwl.writeLock().lock();
        try {
            object = null;
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "CachingReference[" + this.object + "]";
    }
}
