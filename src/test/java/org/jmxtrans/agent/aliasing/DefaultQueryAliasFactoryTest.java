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
