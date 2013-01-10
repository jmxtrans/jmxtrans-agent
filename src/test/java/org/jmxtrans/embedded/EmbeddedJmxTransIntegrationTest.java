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
package org.jmxtrans.embedded;

import org.jmxtrans.embedded.config.ConfigurationParser;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class EmbeddedJmxTransIntegrationTest {
    public static void main(String[] args) throws Exception {
        EmbeddedJmxTransIntegrationTest integrationTest = new EmbeddedJmxTransIntegrationTest();
        integrationTest.integrationTest();
    }

    public void integrationTest() throws Exception {
        EmbeddedJmxTrans embeddedJmxTrans = new ConfigurationParser().newEmbeddedJmxTrans("classpath:org/jmxtrans/embedded/embedded-integ-test.json");
        embeddedJmxTrans.start();

        generateJvmActivity();

        System.out.println("bye");
        embeddedJmxTrans.stop();
    }

    private void generateJvmActivity() throws Exception {
        Random random = new Random();

        for (int i = 0; i < 1000; i++) {

            String msg = "";
            for (int j = 0; j < 100; j++) {
                int[] buffer = new int[2048];
                for (int bufferIdx = 0; bufferIdx < buffer.length; bufferIdx++) {
                    buffer[bufferIdx] = random.nextInt();
                }
                int total = 0;
                for (int aBuffer : buffer) {
                    total += aBuffer;
                }
                msg += total + " ";
                TimeUnit.MILLISECONDS.sleep(10);
            }
            System.out.println(msg);

        }
    }

}
