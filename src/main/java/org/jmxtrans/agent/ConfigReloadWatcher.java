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

import java.util.logging.Level;

import org.jmxtrans.agent.util.logging.Logger;
import org.w3c.dom.Document;

/**
 * Watches a configuration source and notifies a listener when changes are detected.
 * 
 * @author Kristoffer Erlandsson
 */
public final class ConfigReloadWatcher {

    private static final Logger logger = Logger.getLogger(ConfigReloadWatcher.class.getName());

    private Thread watchThread;

    /**
     * @param configurationChangedListener
     *            The listener to notify when changes are detected.
     * @param initialConfiguration
     *            The initial configuration. How often to check for changes are read from here. It will also be the
     *            initial state to compare against when detecting changes.
     * @param configLoader
     *            This is the configuration source, configuration is loaded from here and compared against the current
     *            configuration.
     */
    public ConfigReloadWatcher(ConfigurationChangedListener configurationChangedListener,
            JmxTransExporterConfiguration initialConfiguration,
            ConfigurationDocumentLoader configLoader) {
        watchThread = new Thread(new WatcherRunnable(configurationChangedListener, initialConfiguration, configLoader));
        watchThread.setDaemon(true);
        watchThread.setName("jmxtrans-agent-config-reload-watcher");
    }

    private static class WatcherRunnable implements Runnable {
        private JmxTransExporterConfiguration currentConfiguration;
        private final ConfigurationChangedListener configurationChangedListener;
        private final ConfigurationDocumentLoader configLoader;

        public WatcherRunnable(ConfigurationChangedListener configurationChangedListener,
                JmxTransExporterConfiguration initialConfiguration,
                ConfigurationDocumentLoader configLoader) {
            this.configurationChangedListener = configurationChangedListener;
            this.currentConfiguration = initialConfiguration;
            this.configLoader = configLoader;

        }

        @Override
        public void run() {
            logger.log(Level.INFO, "Starting config reload watcher, will check for configuration changes every "
                    + currentConfiguration.getConfigReloadInterval() + " seconds");
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(calculateSleepMillis(currentConfiguration.getConfigReloadInterval()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                checkAndHandleChangedConfiguration();
                if (currentConfiguration.getConfigReloadInterval() < 0) {
                    // Configuration has been updated with disabling of reload
                    logger.log(Level.INFO, "Config reloading disabled, stopping watcher");
                    return;
                }
            }
            logger.log(Level.INFO, "Config reload watcher interrupted, watcher stopped");
        }

        private int calculateSleepMillis(Integer configReloadInterval) {
            // Sleep 10 ms if reload interval is 0 to avoid busy-wait
            return configReloadInterval != 0 ? configReloadInterval * 1000 : 10;
        }

        private void checkAndHandleChangedConfiguration() {
            Document sourceDocument;
            try {
                sourceDocument = configLoader.loadConfiguration();
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Error when checking if configuration " + configLoader
                                + " changed, current config will be kept",
                        e);
                return;
            }
            if (currentConfigDiffers(sourceDocument)) {
                loadAndNotifyConfigurationChanged(sourceDocument);
            }
        }

        private void loadAndNotifyConfigurationChanged(Document sourceDocument) {
            logger.log(Level.INFO, "Detected changed configuration, will reload " + configLoader);
            JmxTransExporterConfiguration newConfig;
            try {
                newConfig = new JmxTransExporterBuilder().build(sourceDocument);
                configurationChangedListener.configurationChanged(newConfig);
                currentConfiguration = newConfig;
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "Error when reloading configuration " + configLoader + ", current config will be kept", e);
            }
        }

        private boolean currentConfigDiffers(Document sourceDocument) {
            return !sourceDocument.isEqualNode(currentConfiguration.getDocument());
        }
    }

    public void start() {
        watchThread.start();
    }

    public void stop() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

}
