/*
 * Copyright 2008-2012 Xebia and the original author or authors.
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
package org.jmxexporter.util;

import org.jmxexporter.util.concurrent.DiscardingBlockingQueue;
import org.junit.Before;
import org.junit.Test;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

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
