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
import org.jmxexporter.util.jmx.SelfNaming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class ManagedGenericKeyedObjectPool<K, V> extends GenericKeyedObjectPool<K, V> implements ManagedGenericKeyedObjectPoolMBean {

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
}
