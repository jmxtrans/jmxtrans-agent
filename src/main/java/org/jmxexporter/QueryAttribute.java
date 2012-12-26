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

import javax.management.ObjectName;
import java.util.Queue;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public interface QueryAttribute {

    public String getName();

    public String getResultAlias();

    /**
     * @param objectName <code>objectName</code> on which the <code>attribute</code> was obtained.
     * @param value      value of the given attribute. A 'simple' value (String, Number, Date) for a
     *                   {@linkplain QuerySimpleAttribute} or a {@link javax.management.openmbean.CompositeData} for a
     *                   {@linkplain QueryCompositeAttribute}.
     * @param epochInMillis      time at which the metric was collected
     * @param results    queue to which the the computed result(s) must be added
     * @return collected results
     */
    public void performQuery(ObjectName objectName, Object value, long epochInMillis, Queue<QueryResult> results);

    public Query getQuery();

    public void setQuery(Query query);
}
