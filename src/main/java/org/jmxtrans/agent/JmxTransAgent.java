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
import org.jmxtrans.agent.util.logging.Logger;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransAgent {
    private static Logger logger = Logger.getLogger(JmxTransAgent.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
    public static boolean DIAGNOSTIC = Boolean.valueOf(System.getProperty(JmxTransAgent.class.getName() + ".diagnostic", "false"));

    public static void agentmain(String configFile, Instrumentation inst) {
        initializeAgent(configFile);
    }

    public static void premain(String configFile, Instrumentation inst) throws Exception {
        delayedPremain(configFile);
    }

    private static void delayedPremain(final String configFile) {
        final Runnable premain = new Runnable() {
            public void run() {
                initializeAgent(configFile);
            };
        };
        int delay = Integer.parseInt(System.getProperty("jmxtrans.agent.premain.delay", "0"));
        if (delay > 0) {
            System.out.println("jmxtrans agent initialization delayed by " + delay + " seconds");
        }
        scheduler.schedule(premain, delay, TimeUnit.SECONDS);
    }

    private static void initializeAgent(String configFile) {
        System.out.println("org.jmxtrans.agent initialize");
        dumpDiagnosticInfo();
        if (configFile == null || configFile.isEmpty()) {
            String msg = "JmxTransExporter configurationFile must be defined";
            logger.log(Level.SEVERE, msg);
            throw new IllegalStateException(msg);
        }
        JmxTransExporter jmxTransExporter;
        try {
            jmxTransExporter = new JmxTransExporterBuilder().build(configFile);
            //START
            jmxTransExporter.start();
            logger.info("JmxTransAgent started with configuration '" + configFile + "'");
        } catch (Exception e) {
            String msg = "Exception loading JmxTransExporter from '" + configFile + "'";
            logger.log(Level.SEVERE, msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    public static void dumpDiagnosticInfo() {
        if (!JmxTransAgent.DIAGNOSTIC)
            return;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                while (JmxTransAgent.DIAGNOSTIC) {

                    String prefix = new Timestamp(System.currentTimeMillis()) + " [jmxtrans-agent] ";
                    System.out.println(prefix + "JMXTRANS-AGENT DIAGNOSTIC INFO");

                    // CONTEXT
                    System.out.println(prefix + "Logger level: " + Logger.level);

                    // MBEANS
                    Set<ObjectInstance> objectInstances = ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null);
                    List<ObjectName> objectNames = new ArrayList<>();
                    for (ObjectInstance objectInstance : objectInstances) {
                        objectNames.add(objectInstance.getObjectName());
                    }
                    Collections.sort(objectNames);
                    System.out.println(prefix + "ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null)");
                    for (ObjectName objectName : objectNames) {
                        System.out.println(prefix + "\t" + objectName);
                    }

                    System.out.println(prefix + "ENF OF JMXTRANS-AGENT DIAGNOSING INFO");
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
}
