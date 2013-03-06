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

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.util.jmx.JmxUtils2;
import org.jmxtrans.embedded.util.net.SocketOutputStream;
import org.jmxtrans.embedded.util.net.SocketWriter;
import org.jmxtrans.embedded.util.pool.ManagedGenericKeyedObjectPool;
import org.jmxtrans.embedded.util.pool.SocketOutputStreamPoolFactory;
import org.python.core.*;
import org.python.modules.cPickle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://graphite.readthedocs.org/">Graphite</a> implementation of the {@linkplain OutputWriter}.
 * <p/>
 * This implementation uses <a href="http://graphite.readthedocs.org/en/0.9.10/feeding-carbon.html#the-pickle-protocol">
 * Carbon Pickle protocol</a> over TCP/IP.
 * <p/>
 * Settings:
 * <ul>
 * <li>"host": hostname or ip address of the Graphite server. Mandatory</li>
 * <li>"port": listen port for the TCP Plain Text Protocol of the Graphite server.
 * Optional, default value: {@value #DEFAULT_GRAPHITE_SERVER_PORT}.</li>
 * <li>"namePrefix": prefix append to the metrics name.
 * Optional, default value: {@value #DEFAULT_NAME_PREFIX}.</li>
 * <li>"enabled": flag to enable/disable the writer. Optional, default value: <code>true</code>.</li>
 * </ul>
 * <p>All the results of {@link #write(Iterable)} are sent in one single {@code cPickle} message.</p>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class GraphitePickleWriter extends AbstractOutputWriter implements OutputWriter {

    public static final int DEFAULT_GRAPHITE_SERVER_PORT = 2004;
    public static final String DEFAULT_NAME_PREFIX = "servers.#hostname#.";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Metric path prefix. Ends with "." if not empty;
     */
    private String metricPathPrefix;
    private InetSocketAddress graphiteServerSocketAddress;
    private ManagedGenericKeyedObjectPool<InetSocketAddress, SocketOutputStream> socketOutputStreamPool;
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

        logger.info("Start Graphite Pickle writer connected to '{}'...", graphiteServerSocketAddress);

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

        socketOutputStreamPool = new ManagedGenericKeyedObjectPool<InetSocketAddress, SocketOutputStream>(new SocketOutputStreamPoolFactory(), config);

        socketPoolObjectName = JmxUtils2.registerObject(
                socketOutputStreamPool,
                "org.jmxtrans.embedded:Type=SocketPool,Host=" + host + ",Port=" + port + ",Name=GraphiteSocketPool@" + System.identityHashCode(this),
                ManagementFactory.getPlatformMBeanServer());

        if (isEnabled()) {
            try {
                SocketOutputStream socketOutputStream = socketOutputStreamPool.borrowObject(graphiteServerSocketAddress);
                socketOutputStreamPool.returnObject(graphiteServerSocketAddress, socketOutputStream);
            } catch (Exception e) {
                logger.warn("Test Connection: FAILURE to connect to Graphite server '{}'", graphiteServerSocketAddress, e);
            }
        }
        try {
            Class.forName("org.python.modules.cPickle");
        } catch (ClassNotFoundException e) {
            throw new EmbeddedJmxTransException("jython librarie is required by the " + getClass().getSimpleName() +
                    " but is not found in the classpath. Please add org.python:jython:2.5.3+ to the classpath.");
        }
    }

    /**
     * Send given metrics to the Graphite server.
     */
    @Override
    public void write(Iterable<QueryResult> results) {
        logger.debug("Export to '{}' results {}", graphiteServerSocketAddress, results);
        SocketOutputStream socketOutputStream = null;
        try {
            socketOutputStream = socketOutputStreamPool.borrowObject(graphiteServerSocketAddress);
            PyList list = new PyList();

            for (QueryResult result : results) {
                String metricName = metricPathPrefix + result.getName();
                int time = (int) result.getEpoch(TimeUnit.SECONDS);
                PyObject pyValue;
                if (result.getValue() instanceof Integer) {
                    pyValue = new PyInteger((Integer) result.getValue());
                } else if (result.getValue() instanceof Long) {
                    pyValue = new PyLong((Long) result.getValue());
                } else if (result.getValue() instanceof Float) {
                    pyValue = new PyFloat((Float) result.getValue());
                } else if (result.getValue() instanceof Double) {
                    pyValue = new PyFloat((Double) result.getValue());
                } else if (result.getValue() instanceof Date) {
                    pyValue = new PyLong(TimeUnit.SECONDS.convert(((Date) result.getValue()).getTime(), TimeUnit.MILLISECONDS));
                } else {
                    pyValue = new PyString(result.getValue().toString());
                }
                list.add(new PyTuple(new PyString(metricName), new PyTuple(new PyInteger(time), pyValue)));

                logger.debug("Export '{}': ", metricName, result);
            }

            PyString payload = cPickle.dumps(list);

            byte[] header = ByteBuffer.allocate(4).putInt(payload.__len__()).array();

            socketOutputStream.write(header);
            socketOutputStream.write(payload.toBytes());

            socketOutputStream.flush();
            socketOutputStreamPool.returnObject(graphiteServerSocketAddress, socketOutputStream);
        } catch (Exception e) {
            logger.warn("Failure to send result to graphite server '{}' with {}", graphiteServerSocketAddress, socketOutputStream, e);
            if (socketOutputStream != null) {
                try {
                    socketOutputStreamPool.invalidateObject(graphiteServerSocketAddress, socketOutputStream);
                } catch (Exception e2) {
                    logger.warn("Exception invalidating socketWriter connected to graphite server '{}': {}", graphiteServerSocketAddress, socketOutputStream, e2);
                }
            }
        }
    }

    /**
     * Close the {@link SocketWriter} pool.
     */
    @Override
    public void stop() throws Exception {
        logger.info("Stop GraphitePickleWriter connected to '{}' ...", graphiteServerSocketAddress);
        super.stop();
        socketOutputStreamPool.close();
        JmxUtils2.unregisterObject(socketPoolObjectName, ManagementFactory.getPlatformMBeanServer());
    }
}
