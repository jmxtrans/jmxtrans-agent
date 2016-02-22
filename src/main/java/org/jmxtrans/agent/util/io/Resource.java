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

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.annotation.Nonnull;

/**
 * Inspired by {@code org.springframework.core.io.Resource}.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public interface Resource {
    /**
     * Return an {@link InputStream}.
     * <p>It is expected that each call creates a <i>fresh</i> stream.
     * @return the input stream for the underlying resource (must not be {@code null})
     * @throws IoRuntimeException if the stream could not be opened
     */
    @Nonnull
    InputStream getInputStream() throws IoRuntimeException;

    /**
     * Return whether this resource actually exists in physical form.
     * <p>This method performs a definitive existence check, whereas the
     * existence of a {@code Resource} handle only guarantees a
     * valid descriptor handle.
     */
    boolean exists();

    /**
     * Return a URL handle for this resource.
     * @throws IoRuntimeException if the resource cannot be resolved as URL,
     * i.e. if the resource is not available as descriptor
     */
    @Nonnull
    URL getURL() throws IoRuntimeException;

    /**
     * Return a URI handle for this resource.
     * @throws IoRuntimeException if the resource cannot be resolved as URI,
     * i.e. if the resource is not available as descriptor
     */
    @Nonnull
    URI getURI() throws IoRuntimeException;

    /**
     * Return a File handle for this resource.
     * @throws IoRuntimeException if the resource cannot be resolved as absolute
     * file path, i.e. if the resource is not available in a file system
     */
    @Nonnull
    File getFile() throws IoRuntimeException;

    /**
     * Determine the last-modified timestamp for this resource.
     * @throws IoRuntimeException if the resource cannot be resolved
     * (in the file system or as some other known physical resource type)
     */
    long lastModified() throws IoRuntimeException;

    /**
     * Return a description for this resource,
     * to be used for error output when working with the resource.
     * <p>Implementations are also encouraged to return this value
     * from their {@code toString} method.
     * @see Object#toString()
     */
    String getDescription();
}
