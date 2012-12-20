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

import javax.management.Attribute;
import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class QueryAttribute {

    private final Query query;

    private final String name;

    private final String resultAlias;

    public QueryAttribute(Query query, String name) {
        this(query, name, null);
    }

    public QueryAttribute(Query query, String name, String resultAlias) {
        this.query = query;
        this.name = name;
        this.resultAlias = resultAlias;
    }

    public String getName() {
        return name;
    }

    public String getResultAlias() {
        return resultAlias;
    }

    public String getResultName() {
        return getResultAlias() == null ? getName() : getResultAlias();
    }

    public Collection<QueryResult> parseAttribute(Attribute attribute, long epoch) {
        return Collections.singleton(new QueryResult(query.getResultName(), getResultName(), attribute.getValue(), epoch));
    }

    public Query getQuery() {
        return query;
    }
}
