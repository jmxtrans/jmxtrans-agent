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

import org.jmxtrans.agent.util.Preconditions2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class QueryResult {
    @Nonnull
    private final String name;
    private final long epochInMillis;
    @Nullable
    private final Object value;
    @Nullable
    private final String type;

    /**
     * @param name          plain name of the metric (variables (e.g. <code>%my-jmx-attr%</code>) must have been resolved).
     * @param value         value of the collected metric
     * @param epochInMillis collect time in millis (see {@link System#currentTimeMillis()})
     */
    public QueryResult(@Nonnull String name, @Nullable Object value, long epochInMillis) {
        this(name, null, value, epochInMillis);
    }

    /**
     * @param name          plain name of the metric (variables (e.g. <code>%my-jmx-attr%</code>) must have been resolved).
     * @param type          type of the metric (e.g. "{@code counter}", "{@code gauge}", ...)
     * @param value         value of the collected metric
     * @param epochInMillis collect time in millis (see {@link System#currentTimeMillis()})
     */
    public QueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value, long epochInMillis) {
        this.name = Preconditions2.checkNotEmpty(name);
        this.value = value;
        this.epochInMillis = epochInMillis;
        this.type = type;
    }

    public boolean isValueGreaterThan(QueryResult o) {

        if (this.value == null && o.value == null) {
            return false;
        } else if (this.value == null && o.value != null) {
            return false;
        } else if (this.value != null && o.value == null) {
            return true;
        } else if (!(this.value instanceof Number)) {
            throw new IllegalArgumentException("This value is not a number: " + this);
        } else if (!(o.value instanceof Number)) {
            throw new IllegalArgumentException("Other value is not a number: " + this);
        } else if (!this.value.getClass().equals(o.value.getClass())) {
            throw new IllegalArgumentException("Value type mismatch: this.value " + this.value.getClass() + ", o.value " + o.value.getClass());
        }

        if (this.value instanceof Comparable) {
            return ((Comparable) this.value).compareTo(o.value) > 0;
        } else {
            throw new IllegalStateException("this value is not comparable " + this.value.getClass() + " - " + this.toString());
        }
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getType() {
        return type;
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
                "name='" + name + '\'' +
                ", epoch=" + new Timestamp(epochInMillis) +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryResult)) return false;

        QueryResult that = (QueryResult) o;

        if (epochInMillis != that.epochInMillis) return false;
        if (!name.equals(that.name)) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (int) (epochInMillis ^ (epochInMillis >>> 32));
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
