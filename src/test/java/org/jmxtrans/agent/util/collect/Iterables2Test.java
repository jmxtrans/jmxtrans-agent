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
package org.jmxtrans.agent.util.collect;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.*;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Iterables2Test {

    @Test
    public void get_on_list_return_value() {
        List<String> in = Arrays.asList("val0", "val1", "val2", "val3");
        String actual = Iterables2.get(in, 2);
        assertThat(actual, is("val2"));
    }

    @Test
    public void get_on_iterator_return_value() {
        Set<String> in = new TreeSet<String>(Arrays.asList("val0", "val1", "val2", "val3"));
        String actual = Iterables2.get(in, 2);
        assertThat(actual, is("val2"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_negative_position_throws_an_exception() {
        Set<String> in = new HashSet<String>(Arrays.asList("val0", "val1", "val2", "val3"));
        Iterables2.get(in, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_out_of_range_position_throws_an_exception() {
        Set<String> in = new HashSet<String>(Arrays.asList("val0", "val1", "val2", "val3"));
        Iterables2.get(in, 10);
    }
}
