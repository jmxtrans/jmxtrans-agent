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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private IoUtils(){}

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
    public static Document getFileAsDocument(@Nonnull Resource resource) throws IoRuntimeException {
        if (resource == null)
            throw new IoRuntimeException(new NullPointerException("resource cannot be null"));

        DocumentBuilder dBuilder;
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            try {
                File configurationFile = resource.getFile();
                return dBuilder.parse(configurationFile);
            } catch(FileNotFoundRuntimeException e) {
                try (InputStream in = resource.getInputStream()) {
                	return dBuilder.parse(in);
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IoRuntimeException(e);
        } catch (IOException e) {
            throw IoRuntimeException.propagate(e);
        }
    }

    /**
     * Simple implementation without chunking if the source file is big.
     *
     * @param source
     * @param destination
     * @throws java.io.IOException
     */
    private static void doCopySmallFile(File source, File destination) throws IOException {
        if (destination.exists() && destination.isDirectory()) {
            throw new IOException("Can not copy file, destination is a directory: " + destination.getAbsolutePath());
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination, false);
            input = fis.getChannel();
            output = fos.getChannel();
            output.transferFrom(input, 0, input.size());
        } finally {
            closeQuietly(output);
            closeQuietly(input);
            closeQuietly(fis);
            closeQuietly(fos);
        }
        if (destination.length() != source.length()) {
            throw new IOException("Failed to copy content from '" +
                    source + "' (" + source.length() + "bytes) to '" + destination + "' (" + destination.length() + ")");
        }

    }

    public static boolean isFileUrl(URL url){
        if (url.getProtocol().equals("file")) {
            return true;
        } else {
            return false;
        }

    }
    public static void closeQuietly (URLConnection cnn) {
        if (cnn == null) {
            return;
        } else if (cnn instanceof HttpURLConnection) {
            ((HttpURLConnection) cnn).disconnect();
        } else {
            // do nothing
        }
    }
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

    public static void closeQuietly(Writer writer) {
        if (writer == null)
            return;
        try {
            writer.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

    /**
     * Needed for old JVMs where {@link java.io.InputStream} does not implement {@link java.io.Closeable}.
     */
    public static void closeQuietly(InputStream inputStream) {
        if (inputStream == null)
            return;
        try {
            inputStream.close();
        } catch (Exception e) {
            // ignore silently
        }
    }

    public static void replaceFile(File source, File destination) throws IOException {
        boolean destinationExists;
        if (destination.exists()) {
            boolean deleted = destination.delete();
            if (deleted) {
                destinationExists = false;
            } else {
                destinationExists = true;
            }
        } else {
            destinationExists = false;
        }
        if (destinationExists) {
            doCopySmallFile(source, destination);
        } else {
            boolean renamed = source.renameTo(destination);
            if (!renamed) {
                doCopySmallFile(source, destination);
            }
        }
    }

    /**
     * Simple implementation without chunking if the source file is big.
     *
     * @param source
     * @param destination
     * @throws java.io.IOException
     */
    private static void doCopySmallFile(File source, File destination, boolean append) throws IOException {
        if (destination.exists() && destination.isDirectory()) {
            throw new IOException("Can not copy file, destination is a directory: " + destination.getAbsolutePath());
        } else if (!destination.exists()) {
            boolean renamed = source.renameTo(destination);
            if (renamed) return;
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        long initialSize = destination.length();
        try {
            fos = new FileOutputStream(destination, append);
            if (append) {
                fos.write(("\n").getBytes(StandardCharsets.UTF_8));
            }
            fos.write(Files.readAllBytes(Paths.get(source.getAbsolutePath())));
        } finally {
            closeQuietly(output);
            closeQuietly(input);
            closeQuietly(fis);
            closeQuietly(fos);
        }
        if (!append && destination.length() != source.length()) {
            throw new IOException("Failed to copy content from '" +
                    source + "' (" + source.length() + "bytes) to '" + destination + "' (" + destination.length() + "). isAppend? " + append );
        }
        else if (append && destination.length() <= initialSize ) {
            throw new IOException("Failed to append content from '" +
                    source + "' (" + source.length() + "bytes) to '" + destination + "' (" + destination.length() + "). isAppend? " + append );
        }

    }

    public static void appendToFile(File source, File destination, long maxFileSize, int maxBackupIndex) throws IOException {
        boolean destinationExists = validateDestinationFile(source, destination, maxFileSize, maxBackupIndex);
        if (destinationExists) {
            doCopySmallFile(source, destination, true);
        } else {
            boolean renamed = source.renameTo(destination);
            if (!renamed) {
                doCopySmallFile(source, destination, false);
            }
        }
    }

    public static boolean validateDestinationFile(File source, File destination, long maxFileSize, int maxBackupIndex) throws IOException {
        if (!destination.exists() || destination.isDirectory()) return false;
        long totalLengthAfterAppending = destination.length() + source.length();
        if (totalLengthAfterAppending > maxFileSize) {
            rollFiles(destination, maxBackupIndex);
            return false; // File no longer exists because it was move to filename.1
        }

        return true;
    }

    public static void rollFiles(File destination, int maxBackupIndex) throws IOException {

        // if maxBackup index == 10 then we will have file
        // outputFile, outpuFile.1 outputFile.2 ... outputFile.10
        // we only care if 9 and lower exists to move them up a number
        for (int i = maxBackupIndex - 1; i >= 0; i--) {
            String path = destination.getAbsolutePath();
            path=(i==0)?path:path + "." + i;
            File f = new File(path);
            if (!f.exists()) continue;

            File fNext = new File(destination + "." + (i + 1));
            doCopySmallFile(f, fNext, false);
        }

        boolean deleted = destination.delete();
        if (!deleted) {
            logger.warning("Failure to delete file " + destination);
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException{
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) >= 0) {
            out.write(buffer, 0,length);
        }
    }
}
