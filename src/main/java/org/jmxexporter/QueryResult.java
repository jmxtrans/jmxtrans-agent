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
package org.jmxexporter;

import org.jmxexporter.util.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * Value of a collected metric.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
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
