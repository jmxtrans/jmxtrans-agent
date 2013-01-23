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
package org.jmxtrans.embedded.output;

import org.jmxtrans.embedded.QueryResult;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

/**
 * Interface of define a writer that will convert the collected JMX metrics to a given format (e.g. file, Graphite, ...)
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 * @author Jon Stevens
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
     * <p/>
     * Writer are started even if {@link #isEnabled()} is <code>false</code>.
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

