/*
 * Copyright (c) 2010-2014 the original author or authors
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
package org.jmxtrans.agent.aliasing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import javax.management.ObjectName;

import org.jmxtrans.agent.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryAliasFactoryTest {

    private final QueryAliasFactory factory = DefaultQueryAliasFactory.INSTANCE;
    @Mock
    private Query query;

    @Before
    public void initialise() {
        when(query.getAttribute()).thenReturn("queryAttribute");
    }

    @Test
    public void correctAliasIsReturnedWhenKeyIsPresent() throws Exception {
        String objectName = "java.lang:type=GarbageCollector,name=PS Scavenge";
        String escapedObjectName = "java_lang.name__PS_Scavenge.type__GarbageCollector";

        String actual = factory.valueOf(query, new ObjectName(objectName), "key");
        assertThat(actual, is(escapedObjectName + ".queryAttribute.key"));
    }

    @Test
    public void correctAliasIsReturnedWhenKeyIsNotPresent() throws Exception {
        String objectName = "Catalina:type=Resource,resourcetype=Context,path=/,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String escapedObjectName = "Catalina.class__javax_sql_DataSource.host__localhost.name__jdbc_my-datasource.path___.resourcetype__Context.type__Resource";

        String actual = factory.valueOf(query, new ObjectName(objectName), null);
        assertThat(actual, is(escapedObjectName + ".queryAttribute"));
    }

}
