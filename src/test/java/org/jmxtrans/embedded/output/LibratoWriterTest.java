package org.jmxtrans.embedded.output;

import org.jmxtrans.embedded.QueryResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class LibratoWriterTest {
    @Test
    public void testSerialize() throws Exception {

        List<QueryResult> counters = Arrays.asList(
                new QueryResult("counter1", "counter", 10, System.currentTimeMillis()),
                new QueryResult("counter2", "counter", 11.11, System.currentTimeMillis() - 1000),
                new QueryResult("counter2", "counter", 12.12, System.currentTimeMillis())

        );

        List<QueryResult> gauges = Arrays.asList(
                new QueryResult("gauge1", "gauge", 9.9, System.currentTimeMillis()),
                new QueryResult("gauge2", "gauge", 12.12, System.currentTimeMillis() - 1000),
                new QueryResult("gauge2", "gauge", 12.12, System.currentTimeMillis())
        );

        LibratoWriter writer = new LibratoWriter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writer.serialize(counters, gauges, baos);
        baos.flush();

        System.out.println(new String(baos.toByteArray()));

    }
}
