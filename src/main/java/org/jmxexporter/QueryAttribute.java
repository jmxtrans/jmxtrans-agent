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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.ObjectName;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class QueryAttribute implements Comparable<QueryAttribute> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Query query;

    private final String name;

    private final String resultAlias;

    public QueryAttribute(String name) {
        this(name, null);
    }

    public QueryAttribute(String name, String resultAlias) {
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

    /**
     * @param objectName <code>objectName</code> on which the <code>attribute</code> was obtained.
     * @param attribute
     * @param epoch
     * @return
     */
    public Collection<QueryResult> parseAttribute(ObjectName objectName, Attribute attribute, long epoch) {
        Object value = attribute.getValue();
        if (value == null) {
            logger.debug("Ignore null attribute {}", attribute);
            return Collections.emptyList();
        } else if (value instanceof Number || value instanceof String || value instanceof Date) {
            return Collections.singleton(new QueryResult(objectName, getResultName(), value, epoch));
        } else {
            logger.warn("Ignore non String/Number/Date attribute {}", attribute);
            return Collections.emptyList();
        }
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    @Override
    public int compareTo(QueryAttribute attribute) {
        return getResultName().compareTo(attribute.getResultName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryAttribute)) return false;

        QueryAttribute that = (QueryAttribute) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (resultAlias != null ? !resultAlias.equals(that.resultAlias) : that.resultAlias != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (resultAlias != null ? resultAlias.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "QueryAttribute{" +
                "name='" + name + '\'' +
                ", resultAlias='" + resultAlias + '\'' +
                '}';
    }
}
