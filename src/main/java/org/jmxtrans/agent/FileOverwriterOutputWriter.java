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

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jmxtrans.agent.util.ConfigurationUtils.getString;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class FileOverwriterOutputWriter extends AbstractOutputWriter {

    public final static String SETTING_FILE_NAME = "fileName";
    public final static String SETTING_FILE_NAME_DEFAULT_VALUE = "jmxtrans-agent.data";
    private final Logger logger = Logger.getLogger(getClass().getName());
    protected Writer temporaryFileWriter;
    protected File temporaryFile;
    protected File file = new File(SETTING_FILE_NAME_DEFAULT_VALUE);

    @Override
    public synchronized void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);
        file = new File(getString(settings, SETTING_FILE_NAME, SETTING_FILE_NAME_DEFAULT_VALUE));
        logger.info("FileOverwriterOutputWriter configured with file " + file.getAbsolutePath());
    }

    protected Writer getTemporaryFileWriter() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("jmxtrans-agent-", ".data");
            temporaryFile.deleteOnExit();
            if (logger.isLoggable(Level.FINE))
                logger.fine("Created temporary file " + temporaryFile.getAbsolutePath());

            temporaryFileWriter = null;
        }
        if (temporaryFileWriter == null) {
            temporaryFileWriter = new BufferedWriter(new FileWriter(temporaryFile, false));
        }

        return temporaryFileWriter;
    }

    @Override
    public synchronized void writeQueryResult(String metricName, Object value) throws IOException {
        writeResult(metricName, value);
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeResult(invocationName, value);
    }

    protected void writeResult(String name, Object value) throws IOException {
        try {
            getTemporaryFileWriter().write(name + " " + value + "\n");
        } catch (IOException e) {
            releaseTemporaryWriter();
            throw e;
        }
    }

    protected void releaseTemporaryWriter() {
        try {
            IoUtils.closeQuietly(getTemporaryFileWriter());
        } catch (IOException e) {
            // silently skip
        }
        temporaryFile.delete();
        temporaryFile = null;

    }

    @Override
    public synchronized void postCollect() throws IOException {
        try {
            getTemporaryFileWriter().close();
            if (logger.isLoggable(Level.FINE))
                logger.fine("Overwrite " + file.getAbsolutePath() + " by " + temporaryFile.getAbsolutePath());
            IoUtils.replaceFile(temporaryFile, file);
        } finally {
            temporaryFileWriter = null;
        }
    }

    public static class IoUtils {

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

        private static void replaceFile(File source, File destination) throws IOException {
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
    }
}
