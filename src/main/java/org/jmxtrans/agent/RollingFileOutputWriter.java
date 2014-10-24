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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jmxtrans.agent.util.ConfigurationUtils.*;

public class RollingFileOutputWriter extends AbstractOutputWriter {

    public final static String SETTING_FILE_NAME = "fileName";
    public final static String SETTING_FILE_NAME_DEFAULT_VALUE = "jmxtrans-agent.data";
    public final static String SETTING_MAX_FILE_SIZE = "maxFileSize";
    public final static long SETTING_MAX_FILE_SIZE_DEFAULT_VALUE=10;
    public final static String SETTING_MAX_BACKUP_INDEX = "maxBackupIndex";
    public final static int SETTING_MAX_BACKUP_INDEX_DEFAULT_VALUE = 5; 
    private static DateFormat dfISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    
    protected Writer temporaryFileWriter;
    protected File temporaryFile;
    protected File file = new File(SETTING_FILE_NAME_DEFAULT_VALUE);
    protected long maxFileSize;
    protected int maxBackupIndex;

    @Override
    public synchronized void postConstruct(Map<String, String> settings) {
        super.postConstruct(settings);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        dfISO8601.setTimeZone(tz);
        file = new File(getString(settings, SETTING_FILE_NAME, SETTING_FILE_NAME_DEFAULT_VALUE));
        maxFileSize = getLong(settings, SETTING_MAX_FILE_SIZE, SETTING_MAX_FILE_SIZE_DEFAULT_VALUE);
        maxBackupIndex = getInt(settings, SETTING_MAX_BACKUP_INDEX, SETTING_MAX_BACKUP_INDEX_DEFAULT_VALUE);
        // Performance related after 10mb the Filechanel output.transferTo() performs badly
        if (maxFileSize > 10 || maxFileSize < 0) maxFileSize = 10;
        maxFileSize = maxFileSize * 1000000; //converts to bytes.
        logger.log(getInfoLevel(), "RollingFileOutputWriter configured with file " + file.getAbsolutePath());
    }

    protected Writer getTemporaryFileWriter() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("jmxtrans-agent-", ".data");
            temporaryFile.deleteOnExit();
            if (logger.isLoggable(getDebugLevel()))
                logger.log(getDebugLevel(), "Created temporary file " + temporaryFile.getAbsolutePath());

            temporaryFileWriter = null;
        }
        if (temporaryFileWriter == null) {
            temporaryFileWriter = new BufferedWriter(new FileWriter(temporaryFile, false));
        }

        return temporaryFileWriter;
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
        writeQueryResult(invocationName, null, value);
    }

    public synchronized void writeQueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value) throws IOException {
        try {
            getTemporaryFileWriter().write("["+dfISO8601.format(Calendar.getInstance().getTime()) +"] "+name + " " + value + "\n");
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
        if (temporaryFile != null) {
            temporaryFile.delete();
        }
        temporaryFile = null;

    }

    @Override
    public synchronized void postCollect() throws IOException {
        try {
            getTemporaryFileWriter().close();
            if (logger.isLoggable(getDebugLevel()))
                logger.log(getDebugLevel(), "Overwrite " + file.getAbsolutePath() + " by " + temporaryFile.getAbsolutePath());
            IoUtils.appendToFile(temporaryFile, file, maxFileSize, maxBackupIndex);
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
                    fos.write(("\n").getBytes());
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

        private static void appendToFile(File source, File destination, long maxFileSize, int maxBackupIndex) throws IOException {
            boolean destinationExists = validateDestinationFile(source, destination, maxFileSize, maxBackupIndex);
//            if (destination.exists()) {
//                boolean deleted = destination.delete();
//                if (deleted) {
//                    destinationExists = false;
//                } else {
//                    destinationExists = true;
//                }
//            } else {
//                destinationExists = false;
//            }
            if (destinationExists) {
                doCopySmallFile(source, destination, true);
            } else {
                boolean renamed = source.renameTo(destination);
                if (!renamed) {
                    doCopySmallFile(source, destination, false);
                }
            }
        }
        
        private static boolean validateDestinationFile(File source, File destination, long maxFileSize, int maxBackupIndex) throws IOException {
            if (!destination.exists() || destination.isDirectory()) return false;
            long totalLengthAfterAppending = destination.length() + source.length();
            if (totalLengthAfterAppending > maxFileSize) {
                rollFiles(destination, maxBackupIndex);
                return false; // File no longer exists because it was move to filename.1
            }
            
            return true;
        }
        
        private static void rollFiles(File destination, int maxBackupIndex) throws IOException {
            
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
            
            destination.delete();
        }
    }
}

