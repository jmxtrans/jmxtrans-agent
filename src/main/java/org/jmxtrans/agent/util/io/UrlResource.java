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
 */

package org.jmxtrans.agent.util.io;

import org.jmxtrans.agent.util.Preconditions2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * {@link Resource} for {@code http://...}, {@code https://...} or {@code file://...}.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class UrlResource extends AbstractResource implements Resource {
    private final URL url;
    private final URI uri;

    public UrlResource(@Nonnull String url) {
        Preconditions2.checkNotNull(url, "Given url cannot be null");
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IoRuntimeException("Exception parsing URL '" + url + "'", e);
        }
        URI uri;
        try {
            uri = this.url.toURI();
        } catch (URISyntaxException e) {
            throw new IoRuntimeException("Exception parsing URL '" + url + "'", e);
        }
        this.uri = uri;
    }


    @Nonnull
    @Override
    public File getFile() throws IOException {
        if (IoUtils.isFileUrl(url)) {
            return new File(uri);
        } else {
            return super.getFile();
        }
    }

    @Override
    public boolean exists() {
        if (IoUtils.isFileUrl(url)) {
            return super.exists();
        } else {
            URLConnection conn;
            try {
                conn = url.openConnection();
                if (conn instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) conn;
                    configureUrlConnection(httpConn);
                    try {
                        int responseCode = httpConn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            return true;
                        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            return false;
                        } else if (httpConn.getContentLength() > 0) {
                            return true;
                        } else {
                            return false;
                        }
                    } finally {
                        httpConn.disconnect();
                    }
                } else {
                    return super.exists();
                }
            } catch (IOException e) {
                return false;
            }
        }
    }

    @Override
    public long lastModified() throws IOException {
        if (IoUtils.isFileUrl(url)) {
            return super.lastModified();
        } else {
            URLConnection conn = null;
            try {
                conn = url.openConnection();
                configureUrlConnection(conn);
                if (conn instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) conn;
                    int responseCode = httpConn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        return httpConn.getLastModified();
                    } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw new FileNotFoundException(getDescription() + " not found: " +
                                "responseCode=" + responseCode + ", usingProxy=" + httpConn.usingProxy());
                    } else if (responseCode / 100 == 4) {
                        throw new IOException(getDescription() + " is not accessible: " +
                                "responseCode=" + responseCode + ", usingProxy=" + httpConn.usingProxy());
                    } else if (responseCode / 100 == 5) {
                        throw new IOException(getDescription() + " is not available: " +
                                "responseCode=" + responseCode + ", usingProxy=" + httpConn.usingProxy());
                    } else {
                        return 0; // TODO should we fail?
                    }
                } else {
                    return super.lastModified();
                }
            } finally {
                IoUtils.closeQuietly(conn);
            }
        }
    }

    protected void configureUrlConnection(URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection) conn;

            int connectTimeoutInMillis = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
            httpConn.setConnectTimeout(connectTimeoutInMillis);

            int readTimeoutInMillis = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
            httpConn.setReadTimeout(readTimeoutInMillis);
        } else {

        }
    }

    @Nonnull
    @Override
    public InputStream getInputStream() throws IOException {
        URLConnection cnn = url.openConnection();
        configureUrlConnection(cnn);
        try {
            return cnn.getInputStream();
        } catch (IOException e) {
            IoUtils.closeQuietly(cnn);
            throw e;
        }
    }

    @Override
    public String getDescription() {
        return "Http resource: " + url;
    }
}
