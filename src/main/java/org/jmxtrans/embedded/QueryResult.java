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
package org.jmxtrans.embedded;

import org.jmxtrans.embedded.util.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * Value of a collected metric.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 * @author Jon Stevens
 */
public class QueryResult {

    @Nonnull
    private final String name;

    private final long epochInMillis;

    @Nullable
    private final Object value;

    /**
     * @param name          plain name of the metric (variables (e.g. <code>%my-jmx-attr%</code> must have been resolved).
     * @param value         value of the collected metric
     * @param epochInMillis collect time in millis (see {@link System#currentTimeMillis()})
     */
    public QueryResult(@Nonnull String name, @Nullable Object value, long epochInMillis) {
        this.name = Preconditions.checkNotEmpty(name);
        this.value = value;
        this.epochInMillis = epochInMillis;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public long getEpochInMillis() {
        return epochInMillis;
    }

    public long getEpoch(TimeUnit timeUnit) {
        return timeUnit.convert(epochInMillis, TimeUnit.MILLISECONDS);
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                " epoch=" + new Timestamp(epochInMillis) +
                ", name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
