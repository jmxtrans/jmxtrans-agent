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
package org.jmxtrans.embedded.util;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class StringUtils2Test {

    @Test
    public void testDelimitedStringToList() {
        List<String> actual = StringUtils2.delimitedStringToList("a,b;c\nd,,e,;f");
        System.out.println(actual);
        assertThat(actual, IsIterableContainingInOrder.contains("a", "b", "c", "d", "e", "f"));
    }

    @Test
    public void testJoin() {
        List<String> tokens = Arrays.asList("com", "mycompany", "ecommerce", "server1");
        String actual = StringUtils2.join(tokens, ".");
        assertThat(actual, is("com.mycompany.ecommerce.server1"));
    }

    @Test
    public void testReverseTokens() {
        String in = "server1.ecommerce.mycompany.com";
        String actual = StringUtils2.reverseTokens(in, ".");
        assertThat(actual, is("com.mycompany.ecommerce.server1"));
    }
}
