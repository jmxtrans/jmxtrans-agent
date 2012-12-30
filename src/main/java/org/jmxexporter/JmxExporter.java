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
package org.jmxexporter;

import org.jmxexporter.output.OutputWriter;
import org.jmxexporter.util.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p/>
 * <strong>JMX Queries</strong>
 * <p/>
 * If the JMX query returns several mbeans (thanks to '*' or '?' wildcards),
 * then the configured attributes are collected on all the returned mbeans.
 * <p/>
 * <p/>
 * <strong>Output Writers</strong>
 * <p/>
 * {@linkplain OutputWriter}s can be defined at the query level or globally at the {@link JmxExporter} level.
 * The {@linkplain OutputWriter}s that are effective for a {@linkplain Query} are accessible
 * via {@link Query#getEffectiveOutputWriters()}
 * <p/>
 * <p/>
 * <strong>Collected Metrics / Query Results</strong>
 * <p/>
 * Default behavior is to store the query results at the query level (see {@linkplain Query#queryResults}) to resolve the
 * effective {@linkplain OutputWriter}s at result export time ({@linkplain org.jmxexporter.Query#getEffectiveOutputWriters()}).
 * <br/>
 * The drawback is to limit the benefits of batching result
 * to a backend (see {@link org.jmxexporter.Query#exportCollectedMetrics()}) and the size limit of the results list to prevent
 * {@linkplain OutOfMemoryError} in case of export slowness.
 * <p/>
 * An optimization would be, if only one {@linkplain OutputWriter} is defined in the whole {@linkplain JmxExporter}, to
 * replace all the query-local result queues by one global result-queue.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class JmxExporter implements JmxExporterMBean {

    /**
     * Shutdown hook to collect and export metrics a last time if {@link org.jmxexporter.JmxExporter#stop()} was not called.
     */
    private class JmxExporterShutdownHook extends Thread {

        @Override
        public void run() {
            super.run();
            collectMetrics();
            exportCollectedMetrics();
            logger.info("JmxExporterShutdownHook collected and exported metrics");
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ScheduledExecutorService collectScheduledExecutor;

    private ScheduledExecutorService exportScheduledExecutor;

    @Nonnull
    private MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    @Nonnull
    private final List<Query> queries = new ArrayList<Query>();

    /**
     * Use to {@linkplain Set} to deduplicate during configuration merger
     */
    private Set<OutputWriter> outputWriters = new HashSet<OutputWriter>();

    private int numQueryThreads = 1;

    private int numExportThreads = 1;

    private int queryIntervalInSeconds = 15;

    private int exportIntervalInSeconds = 30;

    private int exportBatchSize = 50;

    private JmxExporterShutdownHook shutdownHook = new JmxExporterShutdownHook();

    /**
     * Start the JMX Exporter: initialize underlying queries, start scheduled executors, register shutdown hook
     */
    @PostConstruct
    public void start() throws Exception {

        for (Query query : queries) {
            query.start();
        }
        for (OutputWriter outputWriter : outputWriters) {
            outputWriter.start();
        }

        collectScheduledExecutor = Executors.newScheduledThreadPool(getNumQueryThreads(), new NamedThreadFactory("jmxexporter-collect-"));
        exportScheduledExecutor = Executors.newScheduledThreadPool(getNumExportThreads(), new NamedThreadFactory("jmxexporter-export-"));

        for (final Query query : getQueries()) {
            collectScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    query.collectMetrics();
                }
            }, 0, getQueryIntervalInSeconds(), TimeUnit.SECONDS);
            exportScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    query.exportCollectedMetrics();
                }
            }, 0, getQueryIntervalInSeconds(), TimeUnit.SECONDS);
        }

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }


    /**
     * Stop scheduled executors and collect-and-export metrics one last time.
     */
    @PreDestroy
    public void stop() throws Exception {
        collectScheduledExecutor.shutdown();
        try {
            collectScheduledExecutor.awaitTermination(getQueryIntervalInSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Ignore InterruptedException stopping", e);
        }
        exportScheduledExecutor.shutdown();
        try {
            exportScheduledExecutor.awaitTermination(getExportIntervalInSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Ignore InterruptedException stopping", e);
        }

        collectMetrics();
        exportCollectedMetrics();
        for (Query query : queries) {
            query.stop();
        }
        for (OutputWriter outputWriter : outputWriters) {
            outputWriter.stop();
        }
        logger.info("JMX Exporter stopped. Metrics have been collected and exported one last time.");
        boolean shutdownHookRemoved = Runtime.getRuntime().removeShutdownHook(shutdownHook);
        if (shutdownHookRemoved) {
            logger.debug("ShutdownHook successfully removed");
        } else {
            logger.warn("Failure to remove ShutdownHook");
        }
    }


    /**
     * Exposed for manual / JMX invocation
     */
    @Override
    public void collectMetrics() {
        for (Query query : getQueries()) {
            query.collectMetrics();
        }
    }

    /**
     * Exposed for manual / JMX invocation
     */
    @Override
    public void exportCollectedMetrics() {
        for (Query query : getQueries()) {
            query.exportCollectedMetrics();
        }
    }

    @Nonnull
    public List<Query> getQueries() {
        return queries;
    }

    public void addQuery(@Nonnull Query query) {
        query.setJmxExporter(this);
        this.queries.add(query);
    }

    @Override
    public String toString() {
        return "JmxExporter{" +
                " queries=" + queries +
                ", outputWriters=" + outputWriters +
                ", numQueryThreads=" + numQueryThreads +
                ", queryIntervalInSeconds=" + queryIntervalInSeconds +
                ", numExportThreads=" + numExportThreads +
                ", exportIntervalInSeconds=" + exportIntervalInSeconds +
                ", exportBatchSize=" + exportBatchSize +
                '}';
    }

    public int getNumQueryThreads() {
        return numQueryThreads;
    }

    public void setNumQueryThreads(int numQueryThreads) {
        this.numQueryThreads = numQueryThreads;
    }

    @Override
    public int getQueryIntervalInSeconds() {
        return queryIntervalInSeconds;
    }

    public void setQueryIntervalInSeconds(int queryIntervalInSeconds) {
        this.queryIntervalInSeconds = queryIntervalInSeconds;
    }

    @Override
    public int getExportIntervalInSeconds() {
        return exportIntervalInSeconds;
    }

    public void setExportIntervalInSeconds(int exportIntervalInSeconds) {
        this.exportIntervalInSeconds = exportIntervalInSeconds;
    }

    @Override
    public int getNumExportThreads() {
        return numExportThreads;
    }

    public void setNumExportThreads(int numExportThreads) {
        this.numExportThreads = numExportThreads;
    }

    @Nonnull
    public Set<OutputWriter> getOutputWriters() {
        return outputWriters;
    }

    /**
     * Max number of {@linkplain QueryResult} exported at each call of {@link OutputWriter#write(Iterable)}
     */
    public int getExportBatchSize() {
        return exportBatchSize;
    }

    public void setExportBatchSize(int exportBatchSize) {
        this.exportBatchSize = exportBatchSize;
    }

    @Nonnull
    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    @Override
    public int getCollectedMetricsCount() {
        int result = 0;
        for (Query query : queries) {
            result += query.getCollectedMetricsCount();
        }
        return result;
    }

    @Override
    public long getCollectionDurationInNanos() {
        long result = 0;
        for (Query query : queries) {
            result += query.getCollectionDurationInNanos();
        }
        return result;
    }


    @Override
    public long getCollectionDurationInMillis() {
        return TimeUnit.MILLISECONDS.convert(getCollectionDurationInNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int getCollectionCount() {
        int result = 0;
        for (Query query : queries) {
            result += query.getCollectionCount();
        }
        return result;
    }

    @Override
    public int getExportedMetricsCount() {
        int result = 0;
        for (Query query : queries) {
            result += query.getExportedMetricsCount();
        }
        return result;
    }

    @Override
    public long getExportDurationInNanos() {
        long result = 0;
        for (Query query : queries) {
            result += query.getExportDurationInNanos();
        }
        return result;
    }

    @Override
    public long getExportDurationInMillis() {
        return TimeUnit.MILLISECONDS.convert(getExportDurationInNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int getExportCount() {
        int result = 0;
        for (Query query : queries) {
            result += query.getExportCount();
        }
        return result;
    }

    public int getDiscardedResultsCount() {
        int result = 0;
        for (Query query : queries) {
            int discardedResultsCount = query.getDiscardedResultsCount();
            if (discardedResultsCount != -1) {
                result += discardedResultsCount;
            }
        }
        return result;
    }
}
