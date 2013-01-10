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
package org.jmxtrans.embedded.util.concurrent;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class DiscardingBlockingQueueTest {
    DiscardingBlockingQueue<Integer> queue;

    @Before
    public void before() {
        queue = new DiscardingBlockingQueue<Integer>(5);
    }

    @Test
    public void testAdd() throws Exception {
        for (int i = 0; i < 10; i++) {
            queue.add(i);
        }
        verifyQueueElements();
    }

    private void verifyQueueElements() {
        assertThat(queue.remainingCapacity(), is(0));
        assertThat(queue.poll(), is(5));
        assertThat(queue.poll(), is(6));
        assertThat(queue.poll(), is(7));
        assertThat(queue.poll(), is(8));
        assertThat(queue.poll(), is(9));
    }

    @Test
    public void testOffer() throws Exception {
        for (int i = 0; i < 10; i++) {
            queue.offer(i);
        }
        verifyQueueElements();
    }

    @Test
    public void testPut() throws Exception {
        for (int i = 0; i < 10; i++) {
            queue.put(i);
        }
        verifyQueueElements();
    }

    @Test
    public void testOfferWithTimeout() throws Exception {
        for (int i = 0; i < 10; i++) {
            queue.offer(i, 100, TimeUnit.MILLISECONDS);
        }
        verifyQueueElements();
    }

    @Test
    public void testAddAll() throws Exception {
        List<Integer> integers = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        queue.addAll(integers);

        verifyQueueElements();
    }

    @Test
    public void testRemainingCapacity() throws Exception {
        List<Integer> integers = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        queue.addAll(integers);

        verifyQueueElements();
        queue.put(10);
        queue.put(11);
        assertThat(queue.size(), is(2));
    }
}
