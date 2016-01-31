/*
 * Copyright (c) 2010-2016 the original author or authors
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
package org.jmxtrans.agent.util.io;

import org.jmxtrans.agent.util.logging.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class IoUtils {
    protected final static Logger logger = Logger.getLogger(IoUtils.class.getName());

    /**
     * @param filePath can be prefixed by "{@code http://}", "{@code https://}", "{@code file://}". If given value is not prefixed, "{@code file://}" is assumed.
     * @return @return A <code>long</code> value representing the time the file was
     * last modified, measured in milliseconds since the epoch
     * (00:00:00 GMT, January 1, 1970), or {@code 0L} if the
     * file does not exist, if an I/O error occurs or if the given filePath is {@code null}
     * @throws IoRuntimeException
     */
    public static long getFileLastModificationDate(@Nullable String filePath) throws IoRuntimeException {
        if (filePath == null)
            return 0;

        if (filePath.toLowerCase().startsWith("classpath:")) {
            String classpathResourcePath = filePath.substring("classpath:".length());
            URL configurationFileUrl = Thread.currentThread().getContextClassLoader().getResource(classpathResourcePath);
            if (configurationFileUrl == null) {
                if (logger.isLoggable(Level.FINER))
                    logger.fine("File '" + filePath + "' not found in classpath");
                return 0L;
            } else {
                File configurationFile;
                try {
                    configurationFile = new File(configurationFileUrl.toURI());
                } catch (URISyntaxException e) {
                    throw new IoRuntimeException("Exception parsing '" + filePath + "'", e);
                }
                if (!configurationFile.exists())
                    throw new IllegalStateException("File path=" + filePath + ", url=" + configurationFileUrl + " not found");

                if (logger.isLoggable(Level.FINER))
                    logger.fine("Classpath file '" + filePath + "' last modified at " + new Timestamp(configurationFile.lastModified()));
                return configurationFile.lastModified();
            }
        } else if (
                filePath.toLowerCase().startsWith("http://") ||
                        filePath.toLowerCase().startsWith("https://")
                ) {
            if (logger.isLoggable(Level.FINER))
                logger.fine("Http files not supported: '" + filePath + "' is seen as never modified");
            return 0L;
        } else if (filePath.toLowerCase().startsWith("file://")){
            File file;
            try {
                file = new File(new URI(filePath));
            } catch (URISyntaxException|RuntimeException e) {
                throw new IoRuntimeException("Exception parsing '" + filePath + "'", e);
            }

            if (file.exists()) {
                if (logger.isLoggable(Level.FINER))
                    logger.fine("File '" + filePath + "' last modified at " + new Timestamp(file.lastModified()));
                return file.lastModified();
            } else {
                if (logger.isLoggable(Level.FINER))
                    logger.fine("File '" + filePath + "' not found ");
                return 0L;
            }
        } else {
            File file;
            try {
                file = new File(filePath);
            } catch (RuntimeException e) {
                throw new IoRuntimeException("Exception parsing '" + filePath + "'", e);
            }

            if (file.exists()) {
                if (logger.isLoggable(Level.FINER))
                    logger.fine("File '" + filePath + "' last modified at " + new Timestamp(file.lastModified()));
                return file.lastModified();
            } else {
                if (logger.isLoggable(Level.FINER))
                    logger.fine("File '" + filePath + "' not found ");
                return 0L;
            }
        }
    }

    @Nonnull
    public static Document getFileAsDocument(@Nonnull String configurationFilePath) throws IoRuntimeException {
        if (configurationFilePath == null)
            throw new IoRuntimeException(new NullPointerException("configurationFilePath cannot be null"));

        DocumentBuilder dBuilder;
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();


            if (configurationFilePath.toLowerCase().startsWith("classpath:")) {
                String classpathResourcePath = configurationFilePath.substring("classpath:".length());
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResourcePath);
                return dBuilder.parse(in);
            } else if (configurationFilePath.toLowerCase().startsWith("file://") ||
                    configurationFilePath.toLowerCase().startsWith("http://") ||
                    configurationFilePath.toLowerCase().startsWith("https://")
                    ) {
                URL url = new URL(configurationFilePath);
                return dBuilder.parse(url.openStream());
            } else {
                File xmlFile = new File(configurationFilePath);
                if (!xmlFile.exists()) {
                    throw new IllegalArgumentException("Configuration file '" + xmlFile.getAbsolutePath() + "' not found");
                }
                return dBuilder.parse(xmlFile);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IoRuntimeException(e);
        }
    }
}
