/*
 * Copyright 1012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
