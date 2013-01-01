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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;


import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class EmbeddedJmxTransFactoryTest {
    @Test
    public void testDelimitedStringToList() throws Exception {
        EmbeddedJmxTransFactory factory = new EmbeddedJmxTransFactory();
        String delimitedString = "part1, part2, part3  ;,part4    \n    part5   \r\n   part6";
        List<String> actual = factory.delimitedStringToList(delimitedString);
        assertThat(actual, is(Arrays.asList("part1", "part2", "part3", "part4", "part5", "part6")));
    }


    @Test
    public void testGetObject() throws Exception {
        String configuration = "classpath:org/jmxtrans/embedded/jmxtrans-factory-test.json";
        EmbeddedJmxTransFactory factory = new EmbeddedJmxTransFactory();
        factory.setConfigurationUrl(configuration);

        EmbeddedJmxTrans embeddedJmxTrans = factory.getObject();
        assertThat(embeddedJmxTrans, notNullValue());
        assertThat(embeddedJmxTrans.getQueries().size(), is(8));
        assertThat(embeddedJmxTrans.getOutputWriters().size(), is(1));

        factory.destroy();

    }
}
