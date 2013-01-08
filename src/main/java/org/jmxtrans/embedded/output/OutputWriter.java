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
package org.jmxtrans.embedded.output;

import org.jmxtrans.embedded.QueryResult;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

/**
 * Interface of define a writer that will convert the collected JMX metrics to a given format (e.g. file, Graphite, ...)
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public interface OutputWriter {

    /**
     * Configuration settings of the {@linkplain OutputWriter}.
     */
    Map<String, Object> getSettings();

    /**
     * Sets the configuration setting of the {@linkplain OutputWriter}.
     */
    void setSettings(Map<String, Object> settings);

    /**
     * Write all the given {@linkplain QueryResult} to the target system.
     */
    void write(Iterable<QueryResult> results);

    /**
     * Initialize the {@linkplain OutputWriter}. Called at the startup of the {@linkplain org.jmxtrans.embedded.EmbeddedJmxTrans}.
     * <p/>
     * This is the place to load the configuration (from the injected settings) and to initialize writer's resource like object pools.
     *
     * @throws Exception
     */
    @PostConstruct
    void start() throws Exception;

    /**
     * Stops the underlying resources of the {@linkplain OutputWriter}. Called at the shutdown of the {@linkplain org.jmxtrans.embedded.EmbeddedJmxTrans}.
     * <p/>
     * This is the place to stop the writer's resources like object pools, threads and sockets.
     *
     * @throws Exception
     */
    @PreDestroy
    void stop() throws Exception;

    boolean isEnabled();

    void setEnabled(boolean enabled);
}

