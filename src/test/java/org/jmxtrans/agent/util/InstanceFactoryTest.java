/*
 * Copyright (c) 2010-2014 the original author or authors
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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class InstanceFactoryTest {

    private final InstanceFactory<String> stringInstanceFactory = new InstanceFactory<String>();

    @Test
    public void stringInstanceFactoryReturnsInstancesOfString() {
        assertThat(stringInstanceFactory.newInstanceOf("java.lang.String"), is(instanceOf(String.class)));
    }

    @Test
    public void newInstanceOfStringIsReturnedEveryTime() {
        String stringA = stringInstanceFactory.newInstanceOf("java.lang.String");
        String stringB = stringInstanceFactory.newInstanceOf("java.lang.String");
        assertThat(stringA, is(not(sameInstance(stringB))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentExceptionIsThrownForClassThatDoesNotExist() {
        stringInstanceFactory.newInstanceOf("does.not.Exist");
    }

}
