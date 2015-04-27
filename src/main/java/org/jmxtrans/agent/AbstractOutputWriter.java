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
package org.jmxtrans.agent;

import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import static org.jmxtrans.agent.util.ConfigurationUtils.getString;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public abstract class AbstractOutputWriter implements OutputWriter {

    /**
     * Define the level of log message to display tweaking java.util.logging configuration.
     *
     * Supported values are {@code INFO}
     */
    public final static String SETTING_LOG_LEVEL = "logLevel";
    public final static String SETTING_LOG_LEVEL_DEFAULT_VALUE = "INFO";
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private Level debugLevel = Level.FINE;
    private Level traceLevel = Level.FINER;
    private Level infoLevel = Level.INFO;

    @Override
    public void postConstruct(@Nonnull Map<String, String> settings) {
        String logLevel = getString(settings, SETTING_LOG_LEVEL, SETTING_LOG_LEVEL_DEFAULT_VALUE);
        if ("TRACE".equalsIgnoreCase(logLevel) || "FINEST".equalsIgnoreCase(logLevel)) {
            infoLevel = Level.INFO;
            debugLevel = Level.INFO;
            traceLevel = Level.INFO;
        } else if ("DEBUG".equalsIgnoreCase(logLevel) || "FINER".equalsIgnoreCase(logLevel) || "FINE".equalsIgnoreCase(logLevel)) {
            infoLevel = Level.INFO;
            debugLevel = Level.INFO;
            traceLevel = Level.FINE;
        } else if ("WARN".equalsIgnoreCase(logLevel)) {
            infoLevel = Level.FINE;
            debugLevel = Level.FINE;
            traceLevel = Level.FINE;
        } else {
            infoLevel = Level.INFO;
            debugLevel = Level.FINE;
            traceLevel = Level.FINER;
        }
    }

    @Override
    public void preDestroy() {
    }

    @Override
    public void postCollect() throws IOException {
    }

    @Override
    public void preCollect() throws IOException {
    }

    @Override
    public abstract void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException;

    @Override
    public abstract void writeQueryResult(@Nonnull String metricName, @Nullable String metricType, @Nullable Object value) throws IOException;

    /**
     * To workaround the complex configuration of java.util.logging, we tweak the level for "debug style" messages
     * using the {@value #SETTING_LOG_LEVEL} initialization parameter.
     */
    @Nonnull
    protected Level getDebugLevel() {
        return debugLevel;
    }

    /**
     * To workaround the complex configuration of java.util.logging, we tweak the level for "trace style" messages
     * using the {@value #SETTING_LOG_LEVEL} initialization parameter.
     */
    @Nonnull
    protected Level getTraceLevel() {
        return traceLevel;
    }


    /**
     * To workaround the complex configuration of java.util.logging, we tweak the level for "info style" messages
     * using the {@value #SETTING_LOG_LEVEL} initialization parameter.
     */
    @Nonnull
    protected Level getInfoLevel() {
        return infoLevel;
    }
}
