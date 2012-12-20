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

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class QueryTest {
    static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    @Test
    public void basic_jmx_attribute_return_simple_result() throws Exception {

        Query query = new Query("java.lang:type=MemoryPool,name=PS Eden Space").addAttribute("CollectionUsageThreshold");
        query.performQuery(mbeanServer);
        assertThat(query.getResults().size(), is(1));

        QueryResult result = query.getResults().poll();
        assertThat(result.getValue(), instanceOf(Number.class));
    }

    @Test
    public void test_composite_jmx_attribute() throws Exception {
        Query query = new Query("java.lang:type=MemoryPool,name=PS Perm Gen");
        query.addAttribute(new QueryCompositeAttribute(query, "Usage", null, "committed", "init", "max", "used"));
        query.performQuery(mbeanServer);
        assertThat(query.getResults().size(), is(4));

        QueryResult result1 = query.getResults().poll();
        assertThat(result1.getValue(), instanceOf(Number.class));

        QueryResult result2 = query.getResults().poll();
        assertThat(result2.getValue(), instanceOf(Number.class));
    }

}
