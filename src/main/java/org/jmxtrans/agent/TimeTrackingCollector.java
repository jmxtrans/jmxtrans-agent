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
package org.jmxtrans.agent;

import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

/**
 * Collector that keeps track of when it was last run and which interval it needs to be run at.
 * 
 * @author Kristoffer Erlandsson
 */
public class TimeTrackingCollector {
    private Collector collector;
    private long lastRun = Long.MIN_VALUE;
    private long collectIntervalMillis;

    private static long currentMillis() {
        // Use nanoTime to ensure that events such as daylight savings do not affect the duration calculation.
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    public TimeTrackingCollector(Collector collector, long collectIntervalMillis) {
        this.collector = collector;
        this.collectIntervalMillis = collectIntervalMillis;
    }

    public void setCollector(Collector collector) {
        this.collector = collector;
    }

    public void collectIfEnoughTimeHasPassed(MBeanServer mbeanServer, OutputWriter outputWriter) {
        long currentMillis = currentMillis();
        if (currentMillis >= lastRun + collectIntervalMillis) {
            lastRun = currentMillis;
            collector.collectAndExport(mbeanServer, outputWriter);
        }
    }

    public long getCollectIntervalMillis() {
        return collectIntervalMillis;
    }

}
