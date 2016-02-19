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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class UrlResourceTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void divide() {
        int responseCode = HttpURLConnection.HTTP_FORBIDDEN;
        int responseCodeGroup = responseCode / 100;
        assertThat(responseCodeGroup, equalTo(4));
    }

    @Test
    public void http_url_exist_and_is_modified() throws Exception {
        stubFor(get(urlEqualTo("/exist.txt")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withHeader("Last-Modified", "Mon, 11 Jun 2007 18:53:14 GMT").withBody("exists")));

        String url = "http://127.0.0.1:" + wireMockRule.port() + "/exist.txt";
        UrlResource res = new UrlResource(url);
        assertThat(res.exists(), is(true));
        assertThat(res.lastModified(), greaterThan(0L));

        try {
            File file = res.getFile();
            fail("File should not exist: " + file);
        } catch (FileNotFoundException e) {
            // expected behavior
        }

        InputStream in = res.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtils.copy(in, baos);
        String actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        String expected = "exists";
        assertThat(actual, is(expected));
    }

    @Test
    public void http_url_does_not_exist() throws Exception {
        stubFor(get(urlEqualTo("/does-not-exist.txt")).willReturn(
                aResponse().withStatus(404)));

        String url = "http://127.0.0.1:" + wireMockRule.port() + "/does-not-exist.txt";
        UrlResource res = new UrlResource(url);
        assertThat(res.exists(), is(false));
        try {
            long lastModified = res.lastModified();
            fail("InputStream should not be returned: " + lastModified);
        } catch (FileNotFoundException e) {
            // expected behavior
        }

        try {
            File file = res.getFile();
            fail("File should not exist: " + file);
        } catch (FileNotFoundException e) {
            // expected behavior
        }

        try {
            InputStream in = res.getInputStream();
            fail("InputStream should not be returned: " + in);
        } catch (FileNotFoundException e) {
            // expected behavior
        }
    }

    @Test
    public void file_url_exist_and_is_modified() throws Exception {
        File file = tmp.newFile("test_file_url.txt");
        Files.write(file.toPath(), "hello world".getBytes(StandardCharsets.UTF_8));

        URL url = file.toURI().toURL();
        System.out.println(url);
        UrlResource res = new UrlResource(url.toString());
        assertThat(res.exists(), is(true));
        assertThat(res.lastModified(), greaterThan(0L));
        File actualFile = res.getFile();
        assertThat(actualFile, notNullValue());

        String expected = "hello world";
        InputStream in = res.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IoUtils.copy(in, baos);
        String actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertThat(actual, is(expected));
    }
}
