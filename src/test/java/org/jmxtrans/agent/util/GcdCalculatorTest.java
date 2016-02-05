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
package org.jmxtrans.agent.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author Kristoffer Erlandsson
 */
public class GcdCalculatorTest {

    @Test(expected=IllegalArgumentException.class)
    public void emptyList() throws Exception {
        GcdCalculator.gcd(new ArrayList<Long>());
    }

    @Test
    public void oneNumber() throws Exception {
        assertThat(GcdCalculator.gcd(Arrays.asList(3l)), equalTo(3l));
    }

    @Test
    public void twoNumbers() throws Exception {
        assertThat(GcdCalculator.gcd(Arrays.asList(9l, 3l)), equalTo(3l));
    }

    @Test
    public void manyNumbers() throws Exception {
        assertThat(GcdCalculator.gcd(Arrays.asList(18l, 27l, 81l, 54l)), equalTo(9l));
    }
    
    @Test
    public void primes() throws Exception {
        assertThat(GcdCalculator.gcd(Arrays.asList(7l, 17l)), equalTo(1l));
    }
}
