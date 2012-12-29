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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class TestUtils {

    public static Map<String, QueryAttribute> indexQueryAttributesByAliasOrName(Iterable<QueryAttribute> queryAttributes) {
        Map<String, QueryAttribute> results = new HashMap<String, QueryAttribute>();
        for (QueryAttribute queryAttribute : queryAttributes) {
            String key = queryAttribute.getResultAlias() == null ? queryAttribute.getName() : queryAttribute.getResultAlias();
            results.put(key, queryAttribute);
        }

        return results;
    }

    public static Map<String, Query> indexQueriesByAliasOrName(Iterable<Query> queries) {
        Map<String, Query> results = new HashMap<String, Query>();
        for (Query query : queries) {
            String key = query.getResultAlias() == null ? query.getObjectName().toString() : query.getResultAlias();
            results.put(key, query);
        }
        return results;
    }

}
