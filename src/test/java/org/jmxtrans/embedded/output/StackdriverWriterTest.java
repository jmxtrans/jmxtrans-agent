package org.jmxtrans.embedded.output;

import org.jmxtrans.embedded.QueryResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Basic test for the Stackdriver writer
 */
public class StackdriverWriterTest {
    @Test
    public void testSerialize() throws Exception {

        List<QueryResult> metrics = Arrays.asList(
                new QueryResult("metric1", "counter", 10, System.currentTimeMillis()),
                new QueryResult("metric2", "counter", 11.11, System.currentTimeMillis() - 1000),
                new QueryResult("metric2", "counter", 12.12, System.currentTimeMillis()),
                new QueryResult("metric3", "gauge", 9.9, System.currentTimeMillis()),
                new QueryResult("metric3", "gauge", 12.12, System.currentTimeMillis() - 1000),
                new QueryResult("metric4", "gauge", 12.12, System.currentTimeMillis())

        );
        
        StackdriverWriter writer = new StackdriverWriter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writer.serialize(metrics, baos);
        baos.flush();

        System.out.println(new String(baos.toByteArray()));

    }
}
