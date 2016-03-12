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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.annotation.Nonnull;

/**
 * {@link Resource} for {@code path/to/file.txt} resources.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class FileResource extends AbstractResource implements Resource {
    private final File file;

    public FileResource(File file) {
        Preconditions2.checkNotNull(file, "Given file cannot be null");
        this.file = file;
    }

    public FileResource(String filePath) {
        Preconditions2.checkNotNull(filePath, "Given filePath cannot be null");
        if (filePath.startsWith("file://")) {
            filePath = filePath.substring("file://".length());
        }

        this.file = new File(filePath);
    }

    @Nonnull
    @Override
    public File getFile() {
        return this.file;
    }

    @Nonnull
    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(this.getFile());
        } catch (FileNotFoundException e) {
            throw new FileNotFoundRuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public URL getURL() {
        try {
            return getURI().toURL();
        } catch (MalformedURLException e) {
            throw IoRuntimeException.propagate(e);
        }
    }

    @Nonnull
    @Override
    public URI getURI() {
        return getFile().toURI();
    }

    @Override
    public String getDescription() {
        return "File resource: " + file;
    }
}
