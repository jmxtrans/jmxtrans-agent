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
package org.jmxtrans.embedded.spring;

import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.config.ConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link org.jmxtrans.embedded.EmbeddedJmxTrans} factory for Spring Framework integration.
 * <p/>
 * Default {@linkplain #configurationUrls} :
 * <ul>
 * <li><code>classpath:jmxtrans.json</code>: expected to be provided by the application</li>
 * <li><code>classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json</code>: provided by jmxtrans jar for its internal monitoring</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class EmbeddedJmxTransFactory implements FactoryBean<SpringEmbeddedJmxTrans>, BeanNameAware, DisposableBean {

    private final static String DEFAULT_CONFIGURATION_URL = "classpath:jmxtrans.json, classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> configurationUrls;

    private SpringEmbeddedJmxTrans embeddedJmxTrans;

    private ResourceLoader resourceLoader;

    private boolean ignoreConfigurationNotFound = false;

    private String beanName = "jmxtrans";

    private long lastModified;
    
    private int configurationScanPeriod = 0;

    private Runnable configurationReloader = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(configurationScanPeriod);
                } catch (InterruptedException iex) {
                    logger.error("Error while reloading the configuration. Hot reloading disabled.", iex);
                    return;
                }
                logger.debug("Check configuration last modified.");
                List<Resource> configurations = getConfigurations();
                long configurationLastModified = computeConfigurationLastModified(configurations);
                if (configurationLastModified > lastModified) {
                    logger.info("Reloading configuration.");
                    lastModified = configurationLastModified;
                    try {
                        embeddedJmxTrans.stop();
                        embeddedJmxTrans.getQueries().clear();
                        embeddedJmxTrans.getOutputWriters().clear();
                        loadConfiguration(embeddedJmxTrans, configurations);
                        embeddedJmxTrans.start();
                    } catch (Exception e) {
                        logger.error("Error while reloading the configuration. Embedded JmxTrans disabled.", e);
                        return;
                    }
                }
            }
        }
    };


    /**
     * Computes the last modified date of all configuration files.
     *
     * @param configurations the list of available configurations as Spring resources
     * @return 
     */
    private long computeConfigurationLastModified(List<Resource> configurations) {
        long result = 0;
        for (Resource configuration : configurations) {
        	try {
                long currentConfigurationLastModified = configuration.lastModified();
                if (currentConfigurationLastModified > result) {
                    result = currentConfigurationLastModified;
                }
            } catch (IOException ioex) {
                logger.warn("Error while reading last configuration modification date.", ioex);
            }
        }
        return result;
    }
    
    /**
     * Returns the list of all configuration spring resources.
     * 
     * @return
     */
	private List<Resource> getConfigurations() {
		List<Resource> result = new ArrayList<Resource>();
		for (String delimitedConfigurationUrl : configurationUrls) {
			String[] tokens = StringUtils.commaDelimitedListToStringArray(delimitedConfigurationUrl);
			tokens = StringUtils.trimArrayElements(tokens);
			for (String configurationUrl : tokens) {
				configurationUrl = configurationUrl.trim();
				Resource configuration = resourceLoader.getResource(configurationUrl);
				if (configuration != null && configuration.exists()) {
					result.add(configuration);
				} else if (ignoreConfigurationNotFound) {
					logger.debug("Ignore missing configuration file {}", configuration);
				} else {
					throw new EmbeddedJmxTransException("Configuration file " + configuration + " not found");
				}
			}
		}
		return result;
	}


    @Autowired
    public EmbeddedJmxTransFactory(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public SpringEmbeddedJmxTrans getObject() throws Exception {
        logger.info("Load JmxTrans with configuration '{}'", configurationUrls);
        if (embeddedJmxTrans == null) {

            if (configurationUrls == null) {
                configurationUrls = Collections.singletonList(DEFAULT_CONFIGURATION_URL);
            }

            SpringEmbeddedJmxTrans newJmxTrans = new SpringEmbeddedJmxTrans();
            newJmxTrans.setObjectName("org.jmxtrans.embedded:type=EmbeddedJmxTrans,name=" + beanName);

            List<Resource> configurations = getConfigurations();
            
			loadConfiguration(newJmxTrans, configurations);

			lastModified = computeConfigurationLastModified(configurations);

            embeddedJmxTrans = newJmxTrans;
            logger.info("Created EmbeddedJmxTrans with configuration {})", configurationUrls);
            embeddedJmxTrans.start();
            
            if (configurationScanPeriod > 0) {
            	Thread reloader = new Thread(configurationReloader);
            	reloader.setDaemon(true);
            	logger.info("Configuration reloader created. Scanning every {}ms.", configurationScanPeriod);
            	reloader.start();
            } else {
            	logger.info("Automatic configuration reloading is disabled.");
            }
        }
        return embeddedJmxTrans;
    }

	private void loadConfiguration(SpringEmbeddedJmxTrans jmxTrans, List<Resource> configurations) {
		ConfigurationParser parser = new ConfigurationParser();
		for (Resource configuration : configurations) {
			try {
				parser.mergeEmbeddedJmxTransConfiguration(configuration.getInputStream(), jmxTrans);
			} catch (Exception e) {
				throw new EmbeddedJmxTransException("Exception loading configuration " + configuration, e);
			}
		}
	}

    /**
     * <p>
     * See <a href="http://spring.io/blog/2011/08/09/what-s-a-factorybean">Josh Long: What's a FactoryBean?</a>
     * </p>
     * <quote>
     * One important takeaway here is that it is the <code>FactoryBean</code>, <i>not</i> the factoried object itself,
     * that lives in the Spring container and enjoys the lifecycle hooks and container services. The returned instance
     * is transient - Spring knows nothing about what you’ve returned from <code>getObject()</code>, and will make
     * no attempt to exercise any lifecycle hooks or anything else on it.
     * </quote>
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if (this.embeddedJmxTrans != null) {
            this.embeddedJmxTrans.destroy();
        }
    }

    @Override
    public Class<?> getObjectType() {
        return EmbeddedJmxTrans.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * @param configurationUrl coma delimited list
     */
    public void setConfigurationUrl(String configurationUrl) {
        if (this.configurationUrls == null) {
            this.configurationUrls = new ArrayList<String>();
        }
        this.configurationUrls.add(configurationUrl);
    }

    public void setConfigurationUrls(List<String> configurationUrls) {
        if (this.configurationUrls == null) {
            this.configurationUrls = new ArrayList<String>();
        }
        this.configurationUrls.addAll(configurationUrls);
    }

    public void setIgnoreConfigurationNotFound(boolean ignoreConfigurationNotFound) {
        this.ignoreConfigurationNotFound = ignoreConfigurationNotFound;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }


	public void setConfigurationScanPeriod(int configurationScanPeriod) {
		this.configurationScanPeriod = configurationScanPeriod;
	}
}
