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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class ClasspathResourceTest {

    @Test
    public void classpath_resource_exists() throws Exception {
        ClasspathResource resource = new ClasspathResource("classpath://org/jmxtrans/agent/util/io/classpath-resource.txt");
        assertThat(resource.exists(), is(true));

        assertThat(resource.lastModified(), greaterThan(0L));
        File actualFile = resource.getFile();
        assertThat(actualFile, notNullValue());

        String expected = "hello world";
        InputStream in = resource.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtils.copy(in, baos);
        String actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertThat(actual, is(expected));

    }
}
