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
package org.jmxtrans.embedded.servlet;

import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.config.ConfigurationParser;
import org.jmxtrans.embedded.util.StringUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Bootstrap listener to start up and shut down {@link EmbeddedJmxTrans}.
 * <p/>
 * {@code embedded-jmxtrans} configuration files are specified by a coma/line-break
 * delimited list of jmxtrans json configuration file declared in the {@code web.xml}
 * {@code &lt;context-param&gt;} element named "{@value #CONFIG_LOCATION_PARAM}".
 * <p/>
 * Sample:
 * <code><pre>
 * &lt;web-app ...&gt;
 *   &lt;context-param&gt;
 *     &lt;param-name&gt;jmxtrans.config&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *       classpath:jmxtrans.json
 *       classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json
 *       classpath:org/jmxtrans/embedded/config/tomcat-7.json
 *       classpath:org/jmxtrans/embedded/config/jvm-sun-hotspot.json
 *     &lt;/param-value&gt;
 *   &lt;/context-param&gt;
 *   &lt;listener&gt;
 *     &lt;listener-class&gt;org.jmxtrans.embedded.servlet.EmbeddedJmxTransLoaderListener&lt;/listener-class&gt;
 *   &lt;/listener&gt;
 * &lt;/web-app&gt;
 * </pre>
 * </code>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class EmbeddedJmxTransLoaderListener implements ServletContextListener {

    /**
     * Config param for the embedded-jmxtrans configuration urls.
     */
    public static final String CONFIG_LOCATION_PARAM = "jmxtrans.config";
    public static final String SYSTEM_CONFIG_LOCATION_PARAM = "jmxtrans.system.config";
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private EmbeddedJmxTrans embeddedJmxTrans;
    private ObjectName objectName;
    private MBeanServer mbeanServer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().log("Start embedded-jmxtrans ...");

        mbeanServer = ManagementFactory.getPlatformMBeanServer();

        ConfigurationParser configurationParser = new ConfigurationParser();

        String configuration = configureFromSystemProperty(sce);
        if (configuration == null || configuration.isEmpty()){
            configuration = configureFromWebXmlParam(sce);
            if (configuration == null || configuration.isEmpty()){
                configuration = "classpath:jmxtrans.json, classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json";
            }
        }

        List<String> configurationUrls = StringUtils2.delimitedStringToList(configuration);
        embeddedJmxTrans = configurationParser.newEmbeddedJmxTrans(configurationUrls);
        String on = "org.jmxtrans.embedded:type=EmbeddedJmxTrans,name=jmxtrans,path=" + sce.getServletContext().getContextPath();
        try {
            objectName = mbeanServer.registerMBean(embeddedJmxTrans, new ObjectName(on)).getObjectName();
        } catch (Exception e) {
            throw new EmbeddedJmxTransException("Exception registering '" + objectName + "'", e);
        }
        try {
            embeddedJmxTrans.start();
        } catch (Exception e) {
            String message = "Exception starting jmxtrans for application '" + sce.getServletContext().getContextPath() + "'";
            sce.getServletContext().log(message, e);
            throw new EmbeddedJmxTransException(message, e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().log("Stop embedded-jmxtrans ...");

        try {
            mbeanServer.unregisterMBean(objectName);
        } catch (Exception e) {
            logger.error("Silently skip exception unregistering mbean '" + objectName + "'");
        }
        try {
            embeddedJmxTrans.stop();
        } catch (Exception e) {
            throw new EmbeddedJmxTransException("Exception stopping '" + objectName + "'", e);
        }
    }

    private String configureFromSystemProperty(ServletContextEvent sce){
        String configSystemProperty =
                sce.getServletContext().getInitParameter(SYSTEM_CONFIG_LOCATION_PARAM);

        if (configSystemProperty == null || configSystemProperty.isEmpty()){
            return null;
        }

        String prop = System.getProperty(configSystemProperty);
        if (prop == null || prop.isEmpty()){
            return null;
        }

        return "file:///" + prop;
    }

    private String configureFromWebXmlParam(ServletContextEvent sce){
        return sce.getServletContext().getInitParameter(CONFIG_LOCATION_PARAM);
    }
}
