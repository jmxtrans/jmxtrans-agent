package org.jmxtrans.embedded.output;

import org.apache.commons.io.FileUtils;
import org.jmxtrans.embedded.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This output writer can ONLY be used on a per query basis, not in a global
 * output writer configuration because this aggregates all of the different
 * attributes returned by the query into a single line in a csv file, using the
 * {@link org.jmxtrans.embedded.QueryResult#getName()} as the value of the
 * header.<p/>
 *
 * The query can contain multiple MBeans by pattern matched ObjectName
 * <p/>
 * Example query configuration:
 * <code><pre>
 *  "queries": [
 *  {
 *     "outputWriters": [
 *       {
 *          "@class": "org.jmxtrans.embedded.output.CsvWriter",
 *          "settings": {
 *             "outputFile": "log/jmxlog.csv"
 *           }
 *       }
 *      ],
 *      "objectName": "java.lang:type=Memory",
 *      "resultAlias": "jvm.memory",
 *      "attributes": [
 *         {
 *            "name": "HeapMemoryUsage",
 *            "keys": ["committed", "used"]
 *         },
 *         {
 *            "name": "NonHeapMemoryUsage",
 *            "keys": ["committed", "used"]
 *         }
 *      ]
 *  }
 *]
 * </pre></code>
 *
 * See <a href="https://github.com/jmxtrans/embedded-jmxtrans/wiki/Configuration">embedded-jmxtrans configuration</a> for more details.
 *
 * @author <a href="mailto:ryan.mango.larson@gmail.com">Ryan Larson</a>
 *
 */
public class CsvWriter extends AbstractOutputWriter implements OutputWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ICsvListWriter csv;

    String outputFilePath;

    String[] header;

    private boolean firstWrite = true;

    @Override
    public void start() {
        Writer fileWriter;
        try {
            File outputFile = createOutputFile();
            fileWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
            logger.debug("Started CSV output writer, writing to file: {}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error opening file located at {}", outputFilePath);
            throw new RuntimeException(e);
        }

        csv = new CsvListWriter(fileWriter, CsvPreference.EXCEL_PREFERENCE);
    }

    @Override
    public void stop() throws IOException {
        csv.close();
    }

    @Override
    public void write(Iterable<QueryResult> results) {
        try {
            SortedMap<String, List<QueryResult>> splitResults =
                    splitQueryResultsByTime(results);

            for (Map.Entry<String, List<QueryResult>> entry : splitResults.entrySet()) {
                List<QueryResult> sortedResults = entry.getValue();

                if (firstWrite) {
                    writeHeader(sortedResults);
                }
                writeBody(sortedResults, entry.getKey());
            }
        } catch (IOException e) {
            logger.error("Error writing header to file located at {}", outputFilePath);
            throw new RuntimeException(e);
        }
    }

    private void writeHeader(List<QueryResult> results) throws IOException {
        header = createHeader(results);

        try {
            csv.writeHeader(header);
        } finally {
            csv.flush();
        }
        firstWrite = false;
    }

    private void writeBody(List<QueryResult> results, String epoch) throws IOException {
        List<Object> alignedResults = alignResults(results, epoch);
        try {
            csv.write(alignedResults);
        } finally {
            csv.flush();
        }
    }

    /**
     * Often, query results for a given query
     * come in batches so we need to split them up by time
     *
     * @param results from {@link CsvWriter#write(Iterable)}
     * @return {@link Map} from epoch time in seconds --> results list from that time
     */
    private SortedMap<String, List<QueryResult>> splitQueryResultsByTime(Iterable<QueryResult> results) {
        SortedMap<String, List<QueryResult>> resultsByTime = new TreeMap<String, List<QueryResult>>();

        for (QueryResult result : results) {
            String epoch = String.valueOf(result.getEpoch(TimeUnit.SECONDS));

            if (resultsByTime.containsKey(epoch)) {
                List<QueryResult> current = resultsByTime.get(epoch);
                current.add(result);
                resultsByTime.put(epoch, current);
            } else {
                ArrayList<QueryResult> newQueryList = new ArrayList<QueryResult>();
                newQueryList.add(result);
                resultsByTime.put(epoch, newQueryList);
            }
        }

        return resultsByTime;
    }

    private File createOutputFile() throws IOException {
        //This is funky but we have to do it to support testing
        if (outputFilePath == null) {
            outputFilePath = getStringSetting("outputFile", "jmxCsv.log");
        }
        File outputFile = new File(outputFilePath);
        FileUtils.forceMkdir(outputFile.getAbsoluteFile().getParentFile());
        return outputFile;
    }

    private String[] createHeader(List<QueryResult> results) {
        List<String> headerList = queryResultToHeader(results);
        headerList.add(0, "time");
        return headerList.toArray(new String[headerList.size()]);
    }

    /**
     * We have no guarantee that the results will always be in the same order,
     * so we make sure to align them according to the header on each query.
     */
    List<Object> alignResults(List<QueryResult> results, String epoch) {
        Object[] alignedResults = new Object[results.size() + 1];
        List<String> headerList = Arrays.asList(header);

        alignedResults[0] = epoch;

        for (QueryResult result : results) {
            alignedResults[headerList.indexOf(result.getName())] = result.getValue();
        }

        return Arrays.asList(alignedResults);
    }

    private List<String> queryResultToHeader(List<QueryResult> results) {
        List<String> header = new ArrayList<String>();
        for (QueryResult result : results) {
            header.add(result.getName());
        }
        return header;
    }
}