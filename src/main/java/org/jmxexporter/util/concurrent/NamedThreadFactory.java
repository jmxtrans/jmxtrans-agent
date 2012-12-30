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
package org.jmxexporter.util.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic {@linkplain ThreadFactory} to redifine the name of the created thread.
 * <p/>
 * Inspired by Google Guava's {@link com.google.common.util.concurrent.ThreadFactoryBuilder}
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class NamedThreadFactory implements ThreadFactory {

    private final ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();

    public NamedThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    private String threadNamePrefix;

    private AtomicLong increment = new AtomicLong();

    @Override
    @Nonnull
    public Thread newThread(Runnable r) {
        Thread thread = backingThreadFactory.newThread(r);
        thread.setName(threadNamePrefix + increment.incrementAndGet());
        return thread;
    }
}
