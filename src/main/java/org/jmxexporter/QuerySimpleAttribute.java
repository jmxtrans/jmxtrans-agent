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

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.Date;
import java.util.Queue;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class QuerySimpleAttribute extends AbstractQueryAttribute implements QueryAttribute {

    protected ResultNameStrategy resultNameStrategy = new ResultNameStrategy();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public QuerySimpleAttribute(String name) {
        super(name, null);
    }

    public QuerySimpleAttribute(String name, String resultAlias) {
        super(name, resultAlias);
    }

    @Override
    public void performQuery(ObjectName objectName, Object value, long epochInMillis, Queue<QueryResult> results) {
        if (value == null) {
            logger.debug("Ignore null attribute {}", this);
        } else if (value instanceof Number || value instanceof String || value instanceof Date) {
            String resultName = resultNameStrategy.getResultName(getQuery(), objectName, this);
            QueryResult result = new QueryResult(resultName, value, epochInMillis);
            logger.debug("Collect {}", result);
            results.add(result);
        } else if (value instanceof CompositeData) {
            logger.info("Unexpected compositeData for {}:{}:{}, collect all keys", getQuery(), objectName, this);
            CompositeData compositeData = (CompositeData) value;
            for (String key : compositeData.getCompositeType().keySet()) {
                Object compositeValue = compositeData.get(key);
                if (compositeValue instanceof Number || compositeValue instanceof String || compositeValue instanceof Date) {
                    String resultName = resultNameStrategy.getResultName(getQuery(), objectName, this, key);
                    QueryResult result = new QueryResult(resultName, compositeValue, epochInMillis);
                    logger.debug("Collect {}", result);
                    results.add(result);
                } else {
                    logger.trace("Skip non supported value {}:{}:{}:{}={}", getQuery(), objectName, this, key, compositeValue);
                }
            }
        } else {
            logger.info("Ignore non String/Number/Date attribute {}:{}:{}={}", getQuery(), objectName, this, value);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuerySimpleAttribute)) return false;

        QuerySimpleAttribute that = (QuerySimpleAttribute) o;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getResultAlias() != null ? !getResultAlias().equals(that.getResultAlias()) : that.getResultAlias() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getResultAlias() != null ? getResultAlias().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "QuerySimpleAttribute{" +
                "name='" + getName() + '\'' +
                ", resultAlias='" + getResultAlias() + '\'' +
                '}';
    }
}
