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

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class QueryResult {

    private Query query;

    private final String attributeName;

    private final long epochInMillis;

    private final Object value;

    public QueryResult(String attributeName, Object value, long epochInMillis) {
        this.attributeName = attributeName;
        this.epochInMillis = epochInMillis;
        this.value = value;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public long getEpochInMillis() {
        return epochInMillis;
    }

    public long getEpoch(TimeUnit timeUnit) {
        return timeUnit.convert(epochInMillis, TimeUnit.MILLISECONDS);
    }

    public Object getValue() {
        return value;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "query=" + query == null ? null : query.getResultName() +
                ", attributeName='" + attributeName + '\'' +
                ", epoch=" + new Timestamp(epochInMillis) +
                ", value=" + value +
                '}';
    }
}
