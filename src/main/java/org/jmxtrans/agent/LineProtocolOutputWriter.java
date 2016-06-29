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

import static org.jmxtrans.agent.util.ConfigurationUtils.getInt;
import static org.jmxtrans.agent.util.ConfigurationUtils.getString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jmxtrans.agent.util.net.HostAndPort;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author <a href="tao.shen@transwarp.io">Tao Shen</a>
 */
public class LineProtocolOutputWriter extends AbstractOutputWriter implements OutputWriter {

    public final static String SETTING_HOST = "host";
    public final static String SETTING_PORT = "port";
    public final static String SETTING_NAME_PREFIX = "namePrefix";
    public final static String SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS = "socket.connectTimeoutInMillis";
    public final static int SETTING_PORT_DEFAULT_VALUE = 8086;
    public final static int SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE = 10000;
    private final static int MAX_SEND_TIME_INTERVEL = 300000;
    private final static int MAX_SEND_MSEEAGE_COUNT = 5000;

    protected String metricPathPrefix;
    protected HostAndPort lineProtocolOutputWriter;
    private URL url;
    private int socketConnectTimeoutInMillis = SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE;
    private Map<String, String> localSettings = null;
    private HttpURLConnection urlConnection = null;
    private String Database = null;
    private OutputStream outputStream = null;
    private OutputStreamWriter outputStreamWriter = null;
    private LinkedList<ObjectName> objectNames = new LinkedList<ObjectName>();
    private long firstTime = 0;
    private AtomicInteger countWrite = new AtomicInteger(0);

    @Override
    public void postConstruct(Map<String, String> settings) {
        localSettings = settings;
        lineProtocolOutputWriter = new HostAndPort(getString(settings, SETTING_HOST), getInt(settings, SETTING_PORT, SETTING_PORT_DEFAULT_VALUE));
        metricPathPrefix = getString(settings, SETTING_NAME_PREFIX, null);
        socketConnectTimeoutInMillis = getInt(settings, SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS, SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE);
        logger.log(getInfoLevel(), "Configured with " +
            lineProtocolOutputWriter +
            ", metricPathPrefix=" +
            metricPathPrefix +
            ", socketConnectTimeoutInMillis=" +
            socketConnectTimeoutInMillis);
        String expcetionPath = settings.get("exceptions");
        if (expcetionPath != null) {
            try {
                buildObjectNames(expcetionPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        firstTime = System.currentTimeMillis();
        Database = settings.get("database");
        createDatabase();

    }

    public void buildObjectNames(String configurationFilePath) throws Exception {
        if (configurationFilePath == null) {
            throw new NullPointerException("configurationFilePath cannot be null");
        }
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document;
        if (configurationFilePath.toLowerCase().startsWith("classpath:")) {
            String classpathResourcePath = configurationFilePath.substring("classpath:".length());
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResourcePath);
            document = dBuilder.parse(in);
        } else if (configurationFilePath.toLowerCase().startsWith("file://") ||
            configurationFilePath.toLowerCase().startsWith("http://") ||
            configurationFilePath.toLowerCase().startsWith("https://")) {
            URL url = new URL(configurationFilePath);
            document = dBuilder.parse(url.openStream());
        } else {
            File xmlFile = new File(configurationFilePath);
            if (!xmlFile.exists()) {
                throw new IllegalArgumentException("Configuration file '" + xmlFile.getAbsolutePath() + "' not found");
            }
            document = dBuilder.parse(xmlFile);
        }

        Element rootElement = document.getDocumentElement();
        NodeList exceptionList = rootElement.getChildNodes();
        for (int k = 0; k < exceptionList.getLength(); k++) {
            if (exceptionList.item(k) instanceof Element) {
                Element exceptionElement = (Element) exceptionList.item(k);
                String exceptionName = exceptionElement.getAttribute("name");
                objectNames.add(new ObjectName(exceptionName));
            }
        }
    }

    protected void createDatabase() {
        HttpURLConnection urlConnection1 = null;
        int responseCode = 0;
        String createDatabase = String.format("http://%s:%s/query?q=CREATE+DATABASE+%s", lineProtocolOutputWriter.getHost(), lineProtocolOutputWriter.getPort(),
            Database);
        try {
            URL url1 = new URL(createDatabase);
            urlConnection1 = (HttpURLConnection) url1.openConnection();
            urlConnection1.connect();
            responseCode = urlConnection1.getResponseCode();
            if (responseCode != 200 && responseCode != 204) {
                logger.log(getInfoLevel(), "Fail to  create database with respondCode: " +
                    responseCode +
                    ", metricPathPrefix=" +
                    metricPathPrefix +
                    ", socketConnectTimeoutInMillis=" +
                    socketConnectTimeoutInMillis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection1 != null) {
                urlConnection1.disconnect();
            }

        }
    }

    /**
     * {@link java.net.InetAddress#getLocalHost()} may not be known at JVM
     * startup when the process is launched as a Linux service.
     *
     * @return
     */
    protected String buildMetricPathPrefix() {
        if (metricPathPrefix != null) {
            return metricPathPrefix;
        }
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName().replaceAll("\\.", "_");
        } catch (UnknownHostException e) {
            hostname = "#unknown#";
        }
        metricPathPrefix = "servers." + hostname + ".";
        return metricPathPrefix;
    }

    @Override
    public void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    @Override
    public void writeQueryResult(@Nonnull String metricName, @Nullable String type, @Nullable Object value) throws IOException {
        if (value == null) {
            return;
        }
        String urlStr = String.format("http://%s:%s/write?db=%s&precision=ms", lineProtocolOutputWriter.getHost(), lineProtocolOutputWriter.getPort(),
            Database);
        String tag = "," + localSettings.get("tags");
        String valueStr = null;
        if (value instanceof String) {
            valueStr = "\"" + value + "\"";
        } else {
            valueStr = value.toString();
        }
        String msg = String.format("%s%s value=%s %s%n", metricName.replace('.', '_'), tag.replace('.', '_').replaceAll(" ", ""), valueStr,
            System.currentTimeMillis());
        try {
            ensureLineProtocalConnection(urlStr);
            outputStreamWriter.write(msg);
            countWrite.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(getInfoLevel(), "Fail to send to influxdb server!");
            releaseLineProtocalConnection();
            throw e;
        }
    }

    private void releaseLineProtocalConnection() {
        if (outputStreamWriter != null) {
            try {
                outputStreamWriter.flush();
                outputStreamWriter.close();
                if (urlConnection.getResponseCode() != 200 && urlConnection.getResponseCode() != 204) {
                    logger.log(getInfoLevel(), "HttpResponseCode: " + urlConnection.getResponseCode() + "--" + urlConnection.getResponseMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStreamWriter = null;
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (urlConnection != null) {
            urlConnection.disconnect();
            urlConnection = null;
        }
    }

    private void ensureLineProtocalConnection(String urlStr) throws IOException {
        if (urlConnection != null) {
            return;
        }
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.log(getInfoLevel(), "Malformed url");
        }
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Accept-Charset", "utf-8");
        outputStream = urlConnection.getOutputStream();
        outputStreamWriter = new OutputStreamWriter(outputStream, "utf-8");
    }

    @Override
    public void postCollect() throws IOException {
        registException();
        long currentTime = System.currentTimeMillis();
        if (outputStreamWriter == null) {
            logger.log(getInfoLevel(), "ouputStreamWriter can't be null");
            return;
        }
        if (countWrite.get() >= MAX_SEND_MSEEAGE_COUNT || currentTime - firstTime >= MAX_SEND_TIME_INTERVEL) {
            try {
                outputStreamWriter.flush();
            } catch (IOException e) {
                throw e;
            } finally {
                if (urlConnection.getResponseCode() == 404 && urlConnection.getResponseMessage().equals("Not Found")) {
                    logger.log(getInfoLevel(), "HttpResponseCode: " + urlConnection.getResponseCode() + "--" + urlConnection.getResponseMessage());
                    createDatabase();
                }
                releaseLineProtocalConnection();
                countWrite.set(0);
                firstTime = currentTime;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("LineProtocolOutputWriter{ %s, metricPathPrefix=%s\\}", lineProtocolOutputWriter, metricPathPrefix);
    }

    private void registException() {
        Iterator<ObjectName> iterator = objectNames.iterator();
        while (iterator.hasNext()) {
            ObjectName objectName = iterator.next();
            if (ManagementFactory.getPlatformMBeanServer().isRegistered(objectName)) {
                try {
                    ManagementFactory.getPlatformMBeanServer().addNotificationListener(objectName, new JmxExceptionListener(), null, null);
                    iterator.remove();
                } catch (InstanceNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class JmxExceptionListener implements NotificationListener {
        @Override
        public void handleNotification(Notification notification, Object handback) {
            @Nonnull
            String exceptionName = notification.getType();
            String exceptionMsg = notification.getMessage().replace('\n', '#');
            try {
                writeQueryResult(exceptionName, null, exceptionMsg);
            } catch (IOException e) {
                logger.log(getInfoLevel(), "Write exception message error");
            }
        }

    }
}
