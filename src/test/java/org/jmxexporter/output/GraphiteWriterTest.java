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
package org.jmxexporter.output;

import org.jmxexporter.Query;
import org.jmxexporter.QueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class GraphiteWriterTest {

    GraphiteWriter graphiteWriter;

    @Before
    public void before() {
        graphiteWriter = new GraphiteWriter();
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put(AbstractOutputWriter.SETTING_HOST, "localhost");
        settings.put(AbstractOutputWriter.SETTING_PORT, 2003);

        graphiteWriter.setSettings(settings);
        graphiteWriter.start();
    }

    @After
    public void after() throws Exception {
        graphiteWriter.stop();
    }


    @Test
    public void testWithOneResult() {
        QueryResult queryResult = new QueryResult("singleresult", 10, System.currentTimeMillis());

        Query query = new Query();
        query.setResultAlias("testwithoneresult");
        query.addResult(queryResult);

        graphiteWriter.write(Collections.singleton(queryResult));
    }

    @Test
    public void testWithTwoResult() {
        QueryResult queryResult1 = new QueryResult("first", 10, System.currentTimeMillis());
        QueryResult queryResult2 = new QueryResult("second", 20, System.currentTimeMillis());
        List<QueryResult> results = Arrays.asList(queryResult1, queryResult2);

        Query query = new Query();
        query.setResultAlias("testwithtworesult");
        query.addResults(results);

        graphiteWriter.write(results);
    }
}
