package org.jmxtrans.agent;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JConsoleResultNameStrategyImplTest {
    static JConsoleResultNameStrategyImpl strategy = new JConsoleResultNameStrategyImpl();
    static ExpressionLanguageEngineImpl expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @BeforeClass
    public static void beforeClass() throws Exception {
        strategy.setExpressionLanguageEngine(expressionLanguageEngine);
    }

    @Test
    public void testGetResultName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), "usage", "count", null);
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource.usage.count"));
    }

    @Test
    public void testGetResultNameWithObjectName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), null, null, null);
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource"));
    }

    @Test
    public void testGetResultNameWithNullAttributeName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), "usage", null, null);
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource.usage"));
    }

    @Test
    public void testGetResultNameWithNullKeyName() throws Exception {
        Query query = new Query("*:*", "count", null, strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), null, "count", null);
        assertThat(actual, is("Catalina.javax.sql.DataSource.localhost.jdbc_my-datasource.Context.Resource.count"));
    }

    @Test
    public void testGetResultNameWithResultAlias() throws Exception {
        Query query = new Query("Catalina:*", "count", "Katalina:%name%.%type%", strategy);
        String objectName = "Catalina:type=Resource,resourcetype=Context,host=localhost,class=javax.sql.DataSource,name=\"jdbc/my-datasource\"";
        String actual = strategy.getResultName(query, new ObjectName(objectName), "usage", "count", null);
        assertThat(actual, is("Katalina:jdbc_my-datasource.Resource"));
    }
}
