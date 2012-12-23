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
package org.jmxexporter.output;

import org.jmxexporter.QueryResult;
import org.jmxexporter.util.pool.SocketFactory;
import org.jmxexporter.util.pool.ManagedGenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * See <a hrev="http://graphite.readthedocs.org/en/0.9.10/feeding-carbon.html#the-plaintext-protocol">Graphite / Feeding Carbon / The Plain Text Protocol</a>.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class GraphiteWriter extends AbstractOutputWriter implements OutputWriter {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Metric path prefix. Ends with "." if not empty;
     */
    private String metricPathPrefix;

    private InetSocketAddress graphiteServerSocketAddress;

    private ManagedGenericKeyedObjectPool<InetSocketAddress, Socket> socketPool = new ManagedGenericKeyedObjectPool<InetSocketAddress, Socket>(new SocketFactory());

    private ObjectName socketPoolObjectName;

    @Override
    public void start() {
        int port = getIntSetting(SETTING_PORT, 2003);
        String host = getStringSetting(SETTING_HOST);
        graphiteServerSocketAddress = new InetSocketAddress(host, port);

        logger.info("Start Graphite writer connected to '{}'...", graphiteServerSocketAddress);

        metricPathPrefix = getStringSetting(SETTING_NAME_PREFIX, "servers");
        if (!metricPathPrefix.isEmpty() && !metricPathPrefix.endsWith(".")) {
            metricPathPrefix = metricPathPrefix + ".";
        }

        socketPool = new ManagedGenericKeyedObjectPool<InetSocketAddress, Socket>(new SocketFactory());
        socketPool.setTestOnBorrow(true);
        socketPool.setMaxActive(-1); // no limit
        socketPool.setMaxIdle(-1); // no limit
        socketPool.setTimeBetweenEvictionRunsMillis(TimeUnit.MINUTES.toMillis(5));
        socketPool.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(5));

        try {
            socketPoolObjectName = new ObjectName("org.jmxexporter:Type=SocketPool,Host=" + host + ",port" + port + ",Name=GraphiteSocketPool@" + System.identityHashCode(this));
            ObjectInstance objectInstance = ManagementFactory.getPlatformMBeanServer().registerMBean(socketPool, socketPoolObjectName);
            socketPoolObjectName = objectInstance.getObjectName();
        } catch (Exception e) {
            logger.warn("Silently ignore exception registering mbean {}", socketPoolObjectName, e);
        }

        try {
            Socket socket = socketPool.borrowObject(graphiteServerSocketAddress);
            socketPool.returnObject(graphiteServerSocketAddress, socket);
        } catch (Exception e) {
            logger.warn("Graphite server '{}' connection test failure", e);
        }
    }

    @Override
    public void write(Iterable<QueryResult> results) {
        logger.debug("Export to '{}' results {}", graphiteServerSocketAddress, results);
        Socket socket = null;
        try {
            socket = socketPool.borrowObject(graphiteServerSocketAddress);
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), UTF_8);
            for (QueryResult result : results) {
                String msg = metricPathPrefix + result.getQuery().getResultName() + "." + result.getAttributeName() + " " + result.getEpoch(TimeUnit.SECONDS) + "\n";
                logger.debug("Export '{}'");
                out.write(msg);
            }
            out.flush();
            socketPool.returnObject(graphiteServerSocketAddress, socket);
        } catch (Exception e) {
            logger.warn("Failure to send result to graphite server '{}'", graphiteServerSocketAddress, e);
            if (socket != null) {
                try {
                    socketPool.invalidateObject(graphiteServerSocketAddress, socket);
                } catch (Exception e2) {
                    logger.warn("Exception invalidating socket connected to graphite server '{}'", graphiteServerSocketAddress, e2);
                }
            }
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stop GraphiteWriter connected to '{}' ...", graphiteServerSocketAddress);
        super.stop();
        socketPool.close();
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(socketPoolObjectName);
        } catch (Exception e) {
            logger.warn("Silently ignore exception registering mbean {}", socketPoolObjectName, e);
        }
    }
}
