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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServer;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransExporter {
    /**
     * visible for test
     */
    protected List<Query> queries = new ArrayList<Query>();
    /**
     * visible for test
     */
    protected List<Invocation> invocations = new ArrayList<Invocation>();
    /**
     * visible for test
     */
    protected OutputWriter outputWriter = new DevNullOutputWriter();
    protected int collectInterval = 10;
    protected TimeUnit collectIntervalTimeUnit = TimeUnit.SECONDS;
    private Logger logger = Logger.getLogger(getClass().getName());
    private ThreadFactory threadFactory = new ThreadFactory() {
        final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("jmxtrans-agent-" + counter.incrementAndGet());
            return thread;
        }
    };
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, threadFactory);
    private MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    private ScheduledFuture scheduledFuture;

    public JmxTransExporter withQuery(@Nonnull String objectName, @Nonnull String attribute, @Nullable String resultAlias, 
    		@Nonnull ResultNameStrategy resultNameStrategy) {
        return withQuery(objectName, attribute, null, null, null, resultAlias, resultNameStrategy);
    }

    public JmxTransExporter withQuery(@Nonnull String objectName, @Nonnull String attribute, @Nullable String key,
                                      @Nullable Integer position, @Nullable String type, @Nullable String resultAlias,
                                      @Nonnull ResultNameStrategy resultNameStrategy) {
        queries.add(new Query(objectName, attribute, key, position, type, resultAlias, resultNameStrategy));
        return this;
    }
    public JmxTransExporter withInvocation(@Nonnull String objectName, @Nonnull String operation, @Nullable String resultAlias) {
        invocations.add(new Invocation(objectName, operation, new Object[0], new String[0], resultAlias));
        return this;
    }
    public JmxTransExporter withOutputWriter(OutputWriter outputWriter) {
        this.outputWriter = outputWriter;
        return this;
    }

    public JmxTransExporter withCollectInterval(int collectInterval, @Nonnull TimeUnit collectIntervalTimeUnit) {
        this.collectInterval = collectInterval;
        this.collectIntervalTimeUnit = collectIntervalTimeUnit;
        return this;
    }

    public void start() {
        if (logger.isLoggable(Level.FINER)) {
            logger.fine("starting " + this.toString() + " ...");
        } else {
            logger.fine("starting " + getClass().getName() + " ...");
        }

        if (scheduledFuture != null) {
            throw new IllegalArgumentException("Exporter is already started");
        }
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                collectAndExport();
            }
        }, collectInterval / 2, collectInterval, collectIntervalTimeUnit);

        logger.fine(getClass().getName() + " started");
    }

    public void stop() {
        // cancel jobs
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        scheduledExecutorService.shutdown();

        // one last export
        collectAndExport();

        // wait for stop
        try {
            scheduledExecutorService.awaitTermination(collectInterval, collectIntervalTimeUnit);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        logger.info(getClass().getName() + " stopped.");

    }

    protected void collectAndExport() {
        try {
            outputWriter.preCollect();
            for (Invocation invocation : invocations) {
                try {
                    invocation.invoke(mbeanServer, outputWriter);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Ignore exception invoking " + invocation, e);
                }
            }
            for (Query query : queries) {
                try {
                    query.collectAndExport(mbeanServer, outputWriter);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Ignore exception collecting metrics for " + query, e);
                }
            }
            outputWriter.postCollect();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Ignore exception flushing metrics ", e);
        }
    }

    @Override
    public String toString() {
        return "JmxTransExporter{" +
                "queries=" + queries +
                ", invocations=" + invocations +
                ", outputWriter=" + outputWriter +
                ", collectInterval=" + collectInterval +
                " " + collectIntervalTimeUnit +
                '}';
    }
}
