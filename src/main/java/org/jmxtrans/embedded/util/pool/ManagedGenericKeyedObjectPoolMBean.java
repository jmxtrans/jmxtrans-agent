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
package org.jmxtrans.embedded.util.pool;

/**
 * JMX Mbean interface of the {@linkplain ManagedGenericKeyedObjectPool}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public interface ManagedGenericKeyedObjectPoolMBean<K, V> {

    int getNumTestsPerEvictionRun();

    int getNumActive();

    int getNumIdle();

    int getMinIdle();

    boolean getTestOnBorrow();

    long getTimeBetweenEvictionRunsMillis();

    long getMinEvictableIdleTimeMillis();

    boolean getTestWhileIdle();

    boolean getLifo();

    void clear();

    void clearOldest();

    long getMaxWait();

    int getMaxTotal();

    int getMaxActive();

    int getMaxIdle();
}
