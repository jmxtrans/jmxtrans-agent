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
package org.jmxexporter.output;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.jmxexporter.QueryResult;
import org.jmxexporter.util.jmx.JmxUtils2;
import org.jmxexporter.util.net.SocketWriter;
import org.jmxexporter.util.pool.ManagedGenericKeyedObjectPool;
import org.jmxexporter.util.pool.SocketWriterPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://graphite.readthedocs.org/">Graphite</a> implementation of the {@linkplain OutputWriter}.
 * <p/>
 * This implementation uses <a href="http://graphite.readthedocs.org/en/0.9.10/feeding-carbon.html#the-plaintext-protocol">
 * Carbon Plan Text protocol</a> over TCP/IP.
 * <p/>
 * Settings:
 * <ul>
 * <li>"host": hostname or ip address of the Graphite server. Mandatory</li>
 * <li>"port": listen port for the TCP Plain Text Protocol of the Graphite server.
 * Optional, default value: {@value #DEFAULT_GRAPHITE_SERVER_PORT}.</li>
 * <li>"namePrefix": prefix append to the metrics name.
 * Optional, default value: {@value #DEFAULT_NAME_PREFIX}.</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class GraphiteWriter extends AbstractOutputWriter implements OutputWriter {

    public static final int DEFAULT_GRAPHITE_SERVER_PORT = 2003;

    public static final String DEFAULT_NAME_PREFIX = "servers.#hostname#.";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Metric path prefix. Ends with "." if not empty;
     */
    private String metricPathPrefix;

    private InetSocketAddress graphiteServerSocketAddress;

    private ManagedGenericKeyedObjectPool<InetSocketAddress, SocketWriter> socketWriterPool;

    private ObjectName socketPoolObjectName;

    /**
     * Load settings, initialize the {@link SocketWriter} pool and test the connection to the graphite server.
     * <p/>
     * a {@link Logger#warn(String)} message is emitted if the connection to the graphite server fails.
     */
    @Override
    public void start() {
        int port = getIntSetting(SETTING_PORT, DEFAULT_GRAPHITE_SERVER_PORT);
        String host = getStringSetting(SETTING_HOST);
        graphiteServerSocketAddress = new InetSocketAddress(host, port);

        logger.info("Start Graphite writer connected to '{}'...", graphiteServerSocketAddress);

        metricPathPrefix = getStringSetting(SETTING_NAME_PREFIX, DEFAULT_NAME_PREFIX);
        metricPathPrefix = getStrategy().resolveExpression(metricPathPrefix);
        if (!metricPathPrefix.isEmpty() && !metricPathPrefix.endsWith(".")) {
            metricPathPrefix = metricPathPrefix + ".";
        }

        GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
        config.testOnBorrow = getBooleanSetting("pool.testOnBorrow", true);
        config.testWhileIdle = getBooleanSetting("pool.testWhileIdle", true);
        config.maxActive = getIntSetting("pool.maxActive", -1);
        config.maxIdle = getIntSetting("pool.maxIdle", -1);
        config.minEvictableIdleTimeMillis = getLongSetting("pool.minEvictableIdleTimeMillis", TimeUnit.MINUTES.toMillis(5));
        config.timeBetweenEvictionRunsMillis = getLongSetting("pool.timeBetweenEvictionRunsMillis", TimeUnit.MINUTES.toMillis(5));

        socketWriterPool = new ManagedGenericKeyedObjectPool<InetSocketAddress, SocketWriter>(new SocketWriterPoolFactory("UTF-8"), config);

        socketPoolObjectName = JmxUtils2.registerObject(
                socketWriterPool,
                "org.jmxexporter:Type=SocketPool,Host=" + host + ",Port=" + port + ",Name=GraphiteSocketPool@" + System.identityHashCode(this),
                ManagementFactory.getPlatformMBeanServer());

        try {
            SocketWriter socketWriter = socketWriterPool.borrowObject(graphiteServerSocketAddress);
            socketWriterPool.returnObject(graphiteServerSocketAddress, socketWriter);
        } catch (Exception e) {
            logger.warn("Graphite server '{}' connection test failure", e);
        }
    }

    /**
     * Send given metrics to the Graphite server.
     */
    @Override
    public void write(Iterable<QueryResult> results) {
        logger.debug("Export to '{}' results {}", graphiteServerSocketAddress, results);
        SocketWriter socketWriter = null;
        try {
            socketWriter = socketWriterPool.borrowObject(graphiteServerSocketAddress);
            for (QueryResult result : results) {
                String msg = metricPathPrefix + result.getName() + " " + result.getValue() + " " + result.getEpoch(TimeUnit.SECONDS) + "\n";
                logger.debug("Export '{}'", msg);
                socketWriter.write(msg);
            }
            socketWriter.flush();
            socketWriterPool.returnObject(graphiteServerSocketAddress, socketWriter);
        } catch (Exception e) {
            logger.warn("Failure to send result to graphite server '{}' with {}", graphiteServerSocketAddress, socketWriter, e);
            if (socketWriter != null) {
                try {
                    socketWriterPool.invalidateObject(graphiteServerSocketAddress, socketWriter);
                } catch (Exception e2) {
                    logger.warn("Exception invalidating socketWriter connected to graphite server '{}': {}", graphiteServerSocketAddress, socketWriter, e2);
                }
            }
        }
    }

    /**
     * Close the {@link SocketWriter} pool.
     */
    @Override
    public void stop() throws Exception {
        logger.info("Stop GraphiteWriter connected to '{}' ...", graphiteServerSocketAddress);
        super.stop();
        socketWriterPool.close();
        JmxUtils2.unregisterObject(socketPoolObjectName, ManagementFactory.getPlatformMBeanServer());
    }
}
