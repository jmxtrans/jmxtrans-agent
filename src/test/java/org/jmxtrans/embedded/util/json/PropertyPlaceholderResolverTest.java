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
package org.jmxtrans.embedded.util.json;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class PropertyPlaceholderResolverTest {

    private PropertyPlaceholderResolver resolver = new PropertyPlaceholderResolver();

    @Test
    public void testResolveStringWithSystemProperty() {
        System.setProperty("graphite.host", "graphite-server.private.mycompany.com");
        try {
            String actual = resolver.resolveString("${graphite.host:localhost}");
            assertThat(actual, is("graphite-server.private.mycompany.com"));
        } finally {
            System.getProperties().remove("graphite.host");
        }
    }

    @Test
    public void testResolveComplexStringWithSystemProperty() {
        System.setProperty("graphite.host", "graphite-server.private.mycompany.com");
        try {
            String actual = resolver.resolveString("${graphite.host:localhost}:${graphite.port:2003}");
            assertThat(actual, is("graphite-server.private.mycompany.com:2003"));
        } finally {
            System.getProperties().remove("graphite.host");
        }
    }

    @Test
    public void testResolveStringWithDefaultValue() {
        String actual = resolver.resolveString("${graphite.host:localhost}");
        assertThat(actual, is("localhost"));
    }

}
