/*
 * Copyright 2008-2012 Xebia and the original author or authors.
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
package org.jmxexporter.util.pool;

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
