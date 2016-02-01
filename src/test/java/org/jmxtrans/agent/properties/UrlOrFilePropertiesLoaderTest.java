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
package org.jmxtrans.agent.properties;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmxtrans.agent.properties.UrlOrFilePropertiesLoader.FailedToLoadPropertiesException;
import org.junit.Test;

/**
 * @author Kristoffer Erlandsson
 */
public class UrlOrFilePropertiesLoaderTest {

    @Test
    public void loadFromClasspath() throws Exception {
        PropertiesLoader loader = new UrlOrFilePropertiesLoader("classpath:UrlOrFilePropertiesLoaderTest.properties");
        Map<String, String> properties = loader.loadProperties();
        assertThat(properties.size(), equalTo(2));
        assertThat(properties, allOf(hasEntry("foo", "bar"), hasEntry("foobar", "baz")));
    }
    
    @Test
    public void loadFromFileUrl() throws Exception {
        File file = createTempTestFile();
        PropertiesLoader loader = new UrlOrFilePropertiesLoader("file://" + addSlashIfMissing(file.getAbsolutePath()));
        Map<String, String> properties = loader.loadProperties();
        assertThat(properties.size(), equalTo(1));
        assertThat(properties, hasEntry("test", "testvalue"));
    }

    @Test
    public void loadFromFile() throws Exception {
        File file = createTempTestFile();
        PropertiesLoader loader = new UrlOrFilePropertiesLoader(addSlashIfMissing(file.getAbsolutePath()));
        Map<String, String> properties = loader.loadProperties();
        assertThat(properties.size(), equalTo(1));
        assertThat(properties, hasEntry("test", "testvalue"));
    }

    @Test(expected=FailedToLoadPropertiesException.class)
    public void loadFromUrlNotFound() throws Exception {
        PropertiesLoader loader = new UrlOrFilePropertiesLoader("file:///zvvfds43423ffDSZVFDSAFSDSDFFDSAFVCX");
        loader.loadProperties();
    }
    
    @Test(expected=FailedToLoadPropertiesException.class)
    public void loadFromFileNotFound() throws Exception {
        PropertiesLoader loader = new UrlOrFilePropertiesLoader("/fsdfdsvcvzcfdsjkgljl12341");
        loader.loadProperties();
    }

    @Test(expected=FailedToLoadPropertiesException.class)
    public void loadFromClasspathNotFound() throws Exception {
        PropertiesLoader loader = new UrlOrFilePropertiesLoader("classpath:fsdfdsvcvzcfdsjkgljl12341");
        loader.loadProperties();
    }
    

    private File createTempTestFile() throws IOException, FileNotFoundException {
        Path tmpFile = Files.createTempFile(this.getClass().getSimpleName(), null);
        File file = tmpFile.toFile();
        file.deleteOnExit();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write("test=testvalue".getBytes());
        }
        return file;
    }


    private String addSlashIfMissing(String absolutePath) {
        if (absolutePath.startsWith("/")) {
            return absolutePath;
        } else {
            return "/" + absolutePath;
        }
    }
}
