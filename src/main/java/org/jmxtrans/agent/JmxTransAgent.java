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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jmxtrans.agent.properties.NoPropertiesSourcePropertiesLoader;
import org.jmxtrans.agent.properties.PropertiesLoader;
import org.jmxtrans.agent.properties.ResourcePropertiesLoader;
import org.jmxtrans.agent.util.StringUtils2;
import org.jmxtrans.agent.util.io.Resource;
import org.jmxtrans.agent.util.io.ResourceFactory;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransAgent {
    private static Logger logger = Logger.getLogger(JmxTransAgent.class.getName());

    private static final String PROPERTIES_SYSTEM_PROPERTY_NAME = "jmxtrans.agent.properties.file";


    private JmxTransAgent(){}

    @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
    public static boolean DIAGNOSTIC = Boolean.valueOf(System.getProperty(JmxTransAgent.class.getName() + ".diagnostic", "false"));

    public static void agentmain(String configFile, Instrumentation inst) {
        initializeAgent(configFile);
    }

    public static void premain(final String configFile, Instrumentation inst) {
        final int delayInSecs = Integer.parseInt(System.getProperty("jmxtrans.agent.premain.delay", "0"));
        final boolean waitForMBeanServer =
            Boolean.parseBoolean(System.getProperty("jmxtrans.agent.premain.waitForMBeanServer"));
        final int timeoutInSeconds = Integer.parseInt(
            System.getProperty("jmxtrans.agent.premain.waitForMBeanServer.timeoutInSeconds", "120"));

        if (delayInSecs > 0) {
            logger.info("jmxtrans agent initialization delayed by " + delayInSecs + " seconds");
            new Thread("jmxtrans-agent-delayed-starter-" + delayInSecs + "secs") {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delayInSecs * 1000);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        return;
                    }

                    if (waitForMBeanServer) {
                        if (!waitForMBeanServer(timeoutInSeconds)) {
                            return;
                        }
                    }

                    initializeAgent(configFile);
                }
            }.start();
        } else if (waitForMBeanServer) {
            logger.info("jmxtrans agent initialization delayed waiting for MBeanServer");
            new Thread("jmxtrans-agent-delayed-starter-waitForMBeanServer") {
                @Override
                public void run() {
                    if (!waitForMBeanServer(timeoutInSeconds)) {
                        return;
                    }

                    initializeAgent(configFile);
                }
            }.start();
        } else {
            initializeAgent(configFile);
        }
    }

    public static void main(String[] args) {
        Logger.out.println(getVersionInfo());
    }

    /**
     * Human readable name and version.
     *
     * @return project name and version or "{@code jmxtrans-agent}" if package information not found
     */
    @Nonnull
    public static String getVersionInfo() {
        Package pkg = JmxTransAgent.class.getPackage();
        if (pkg == null) {
            return "jmxtrans-agent";
        } else {
            return pkg.getImplementationTitle() + ": " + pkg.getImplementationVersion();
        }
    }

    private static void initializeAgent(String configPath) {
        dumpDiagnosticInfo();
        if (configPath == null || configPath.isEmpty()) {
            String msg = "JmxTransExporter configurationFile must be defined";
            logger.log(Level.SEVERE, msg);
            throw new IllegalStateException(msg);
        }
        try {
            logger.info("Starting '" + getVersionInfo() + "' with configuration '" + configPath + "'...");

            PropertiesLoader propertiesLoader = createPropertiesLoader();
            Resource configuration = ResourceFactory.newResource(configPath);
            ExpressionLanguageEngine expressionLanguageEngine = new ExpressionLanguageEngineImpl();
            JmxTransConfigurationLoader configurationLoader = new JmxTransConfigurationXmlLoader(configuration, propertiesLoader, expressionLanguageEngine);
            JmxTransExporter jmxTransExporter = new JmxTransExporter(configurationLoader);
            //START
            jmxTransExporter.start();
            logger.info("JmxTransAgent started with configuration '" + configPath + "'");
        } catch (Exception e) {
            String msg = "Exception loading JmxTransExporter from '" + configPath + "'";
            logger.log(Level.SEVERE, msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    private static PropertiesLoader createPropertiesLoader() {
        String propertiesFile = System.getProperty(PROPERTIES_SYSTEM_PROPERTY_NAME);
        PropertiesLoader result;
        if (StringUtils2.isNullOrEmpty(propertiesFile)) {
            result = new NoPropertiesSourcePropertiesLoader();
        } else {
            result = new ResourcePropertiesLoader(propertiesFile);
        }
        logger.info("PropertiesLoader: " + result.getDescription());
        return result;
    }

    public static void dumpDiagnosticInfo() {
        if (!JmxTransAgent.DIAGNOSTIC)
            return;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                while (JmxTransAgent.DIAGNOSTIC) {

                    String prefix = new Timestamp(System.currentTimeMillis()) + " [jmxtrans-agent] ";
                    Logger.out.println(prefix + "JMXTRANS-AGENT DIAGNOSTIC INFO");

                    // CONTEXT
                    Logger.out.println(prefix + "Logger level: " + Logger.level);

                    // MBEANS
                    Set<ObjectInstance> objectInstances = ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null);
                    List<ObjectName> objectNames = new ArrayList<>();
                    for (ObjectInstance objectInstance : objectInstances) {
                        objectNames.add(objectInstance.getObjectName());
                    }
                    Collections.sort(objectNames);
                    Logger.out.println(prefix + "ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null)");
                    for (ObjectName objectName : objectNames) {
                        Logger.out.println(prefix + "\t" + objectName);
                    }

                    Logger.out.println(prefix + "ENF OF JMXTRANS-AGENT DIAGNOSING INFO");
                    try {
                        Thread.sleep(TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setName("jmxtrans-agent-diagnostic");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Polls every second to see if any {@link javax.management.MBeanServer} have been created
     * by another thread up to {@code timeoutInSeconds}. If interrupted or timed out, returns
     * {@code false}.
     * @param timeoutInSeconds Maximum number of seconds to wait before giving up.
     * @return {@code true} if found an {@code MBeanServer} within {@code timeoutInSeconds}.
     * {@code false} otherwise.
     */
    private static boolean waitForMBeanServer(int timeoutInSeconds) {
        long startInMs = System.currentTimeMillis();

        while (!isMBeanServerCreated() && secondsSince(startInMs) < timeoutInSeconds) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
                return false;
            }
        }

        if (secondsSince(startInMs) >= timeoutInSeconds) {
            logger.info("jmxagent initialization timed out waiting for MBeanServer");
            return false;
        }

        return true;
    }

    static private boolean isMBeanServerCreated() {
        return MBeanServerFactory.findMBeanServer(null).size() > 0;
    }

    private static long secondsSince(long startInMs) {
        return (System.currentTimeMillis() - startInMs) / 1000;
    }
}
