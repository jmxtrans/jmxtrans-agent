package org.jmxtrans.embedded.output;

import org.jmxtrans.embedded.QueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ryan.mango.larson@gmail.com">Ryan Larson</a>
 */
public class CsvWriterTest {

    CsvWriter csvWriter;
    static File temp;

    @Before
    public void setUp() throws Exception {
        temp = new File("csvWriterTest.txt");
        csvWriter = new CsvWriter();
        csvWriter.outputFilePath = temp.getPath();
        csvWriter.start();
    }

    @After
    public void tearDown() throws Exception {
        csvWriter.stop();
        temp.delete();
    }

    @Test
    public void testCreatesCorrectHeader() {
        csvWriter.write(
                makeQueryResults("first:1", "second:2", "third:3", "fourth:4")
        );

        assertEquals(csvWriter.header[0], "time");
        assertEquals(csvWriter.header[1], "first");
        assertEquals(csvWriter.header[2], "second");
        assertEquals(csvWriter.header[3], "third");
        assertEquals(csvWriter.header[4], "fourth");
    }

    @Test
    public void testAlignResultsRealignsScrambledResults() {
        csvWriter.write(
                makeQueryResults("first:1", "second:2", "third:3", "fourth:4")
        );

        List<Object> alignedResults = csvWriter.alignResults(
                makeQueryResults("second:2", "first:1", "fourth:4", "third:3"), "123456"
        );

        assertEquals(alignedResults.get(0), "123456");
        assertEquals(alignedResults.get(1), "1");
        assertEquals(alignedResults.get(2), "2");
        assertEquals(alignedResults.get(3), "3");
        assertEquals(alignedResults.get(4), "4");
    }

    @Test
    public void testAlignResultsPreservesCorrectlyAlignedResults() {
        csvWriter.write(
                makeQueryResults("first:1", "second:2", "third:3", "fourth:4")
        );

        List<Object> alignedResults = csvWriter.alignResults(
                makeQueryResults("first:1", "second:2", "third:3", "fourth:4"), "123456"
        );

        assertEquals(alignedResults.get(0), "123456");
        assertEquals(alignedResults.get(1), "1");
        assertEquals(alignedResults.get(2), "2");
        assertEquals(alignedResults.get(3), "3");
        assertEquals(alignedResults.get(4), "4");
    }

    @Test
    public void testCsvFileIsWrittenOutCorrectly() throws Exception {
        csvWriter.write(
                makeQueryResults("first:1", "second:2", "third:3", "fourth:4")
        );

        csvWriter.write(
                makeQueryResults("second:2", "first:1", "fourth:4", "third:3")
        );

        csvWriter.write(
                makeQueryResults("first:1", "second:2", "third:3", "fourth:4")
        );


        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(temp)));
        try {
            assertEquals(reader.readLine(), "time,first,second,third,fourth");
            assertEquals(reader.readLine(), "123456,1,2,3,4");
            assertEquals(reader.readLine(), "123456,1,2,3,4");
            assertEquals(reader.readLine(), "123456,1,2,3,4");
        } finally {
            reader.close();
        }
    }

    List<QueryResult> makeQueryResults(String... keyValues) {
        List<QueryResult> results = new ArrayList<QueryResult>();

        for (String keyValue : keyValues) {
            results.add(makeQueryResult(keyValue.split(":")[0], keyValue.split(":")[1]));
        }

        return results;
    }

    private QueryResult makeQueryResult(String name, String value) {
        return new QueryResult(name, value, 123456789L);
    }
}
