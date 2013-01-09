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
package org.jmxtrans.embedded.util.pool;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * JMX Enabled {@linkplain GenericKeyedObjectPool}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ManagedGenericKeyedObjectPool<K, V> extends GenericKeyedObjectPool<K, V> implements ManagedGenericKeyedObjectPoolMBean<K,V> {

    public ManagedGenericKeyedObjectPool() {
        super();
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory) {
        super(factory);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, Config config) {
        super(factory, config);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive) {
        super(factory, maxActive);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        super(factory, maxActive, whenExhaustedAction, maxWait);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        super(factory, maxActive, whenExhaustedAction, maxWait, testOnBorrow, testOnReturn);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, testOnBorrow, testOnReturn);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, maxTotal, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, maxTotal, minIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    public ManagedGenericKeyedObjectPool(KeyedPoolableObjectFactory<K, V> factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle, boolean lifo) {
        super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, maxTotal, minIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle, lifo);
    }

    @Override
    public int getNumIdle() {
        return super.getNumIdle();
    }

    @Override
    public int getNumActive() {
        return super.getNumActive();
    }

    @Override
    public boolean getLifo() {
        return super.getLifo();
    }

    @Override
    public boolean getTestWhileIdle() {
        return super.getTestWhileIdle();
    }

    @Override
    public long getMinEvictableIdleTimeMillis() {
        return super.getMinEvictableIdleTimeMillis();
    }

    @Override
    public int getNumTestsPerEvictionRun() {
        return super.getNumTestsPerEvictionRun();
    }

    @Override
    public long getTimeBetweenEvictionRunsMillis() {
        return super.getTimeBetweenEvictionRunsMillis();
    }

    @Override
    public boolean getTestOnReturn() {
        return super.getTestOnReturn();
    }

    @Override
    public boolean getTestOnBorrow() {
        return super.getTestOnBorrow();
    }

    @Override
    public int getMinIdle() {
        return super.getMinIdle();
    }

    @Override
    public int getMaxIdle() {
        return super.getMaxIdle();
    }

    @Override
    public long getMaxWait() {
        return super.getMaxWait();
    }

    @Override
    public int getMaxTotal() {
        return super.getMaxTotal();
    }

    @Override
    public int getMaxActive() {
        return super.getMaxActive();
    }
}
