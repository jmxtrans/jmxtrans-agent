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
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.jmxtrans.agent.util.ConfigurationUtils.getBoolean;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class OutputWriterCircuitBreakerDecorator implements OutputWriter {
    public final static String SETTING_ENABLED = "enabled";
    protected final Logger logger;
    protected final OutputWriter delegate;
    private boolean enabled = true;
    private int maxFailures = 5;
    private long disableDurationInMillis = 60 * 1000;
    private AtomicInteger failuresCounter = new AtomicInteger();
    private long disabledUntil = 0;

    public OutputWriterCircuitBreakerDecorator(OutputWriter delegate) {
        this.delegate = delegate;
        logger = Logger.getLogger(delegate.getClass().getName() + "CircuitBreaker");
    }

    @Override
    public void postConstruct(Map<String, String> settings) {
        enabled = getBoolean(settings, SETTING_ENABLED, true);
        delegate.postConstruct(settings);
    }

    @Override
    public void preDestroy() {
        delegate.preDestroy();
    }

    @Override
    public void preCollect() throws IOException {
        if (isDisabled()) {
            return;
        }
        try {
            delegate.preCollect();
            incrementOutputWriterSuccess();
        } catch (RuntimeException e) {
            incrementOutputWriterFailures();
            throw e;
        } catch (IOException e) {
            incrementOutputWriterFailures();
            throw e;
        }
    }

    @Override
    public void writeQueryResult(@Nonnull String metricName, @Nullable String metricType, @Nullable Object value) throws IOException {
        if (isDisabled()) {
            return;
        }
        try {
            delegate.writeQueryResult(metricName, metricType, value);
            incrementOutputWriterSuccess();
        } catch (RuntimeException e) {
            incrementOutputWriterFailures();
            throw e;
        } catch (IOException e) {
            incrementOutputWriterFailures();
            throw e;
        }
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        if (isDisabled()) {
            return;
        }
        try {
            delegate.writeInvocationResult(invocationName, value);
            incrementOutputWriterSuccess();
        } catch (RuntimeException e) {
            incrementOutputWriterFailures();
            throw e;
        } catch (IOException e) {
            incrementOutputWriterFailures();
            throw e;
        }
    }

    @Override
    public void postCollect() throws IOException {
        if (isDisabled()) {
            return;
        }
        try {
            delegate.postCollect();
            incrementOutputWriterSuccess();
        } catch (RuntimeException e) {
            incrementOutputWriterFailures();
            throw e;
        } catch (IOException e) {
            incrementOutputWriterFailures();
            throw e;
        }
    }

    public boolean isDisabled() {
        if (!enabled) {
            logger.finer("OutputWriter is globally disabled");
            return true;
        } else if (disabledUntil == 0) {
            logger.finer("OutputWriter is not temporarily disabled");
            return false;
        } else if (disabledUntil < System.currentTimeMillis()) {
            logger.fine("re-enable OutputWriter");
            // reset counter
            disabledUntil = 0;
            return false;
        } else {
            if (logger.isLoggable(Level.FINE))
                logger.fine("OutputWriter is disabled until " + new Timestamp(disabledUntil));
            return true;
        }
    }

    public void incrementOutputWriterFailures() {
        int failuresCount = failuresCounter.incrementAndGet();
        if (failuresCount >= maxFailures) {
            disabledUntil = System.currentTimeMillis() + disableDurationInMillis;
            failuresCounter.set(0);
            logger.warning("Too many exceptions, disable writer until " + new Timestamp(disabledUntil));
        }
    }

    public void incrementOutputWriterSuccess() {
        if (failuresCounter.get() > 0) {
            logger.fine("Reset failures counter to 0");
            failuresCounter.set(0);
        }
    }
}
