package org.jmxtrans.embedded.output;

/**
 * This output writer can ONLY be used on a per query basis, not in a global
 * output writer configuration because this aggregates all of the different
 * attributes returned by the query into a single line in a csv file, using the
 * {@link org.jmxtrans.embedded.QueryResult#getName()} as the value of the
 * header.
 *
 * The query can contain multiple MBeans by pattern matched ObjectName
 *
 * Example query configuration:
 *
   "queries": [
   {
      "outputWriters": [
        {
           "@class": "com.tripwire.tap.libs.jmx.CsvWriter",
           "settings": {
              "outputFile": "log/jmxlog.csv"
            }
        }
       ],
       "objectName": "java.lang:type=Memory",
       "resultAlias": "jvm.memory",
       "attributes": [
          {
             "name": "HeapMemoryUsage",
             "keys": ["committed", "used"]
          },
          {
             "name": "NonHeapMemoryUsage",
             "keys": ["committed", "used"]
          }
       ]
   }
 ]
 *
 * @see https://github.com/jmxtrans/embedded-jmxtrans/wiki/Configuration
 *
 * @author <a href="mailto:ryan.mango.larson@gmail.com">Ryan Larson</a>
 *
 */

import org.apache.commons.io.FileUtils;
import org.jmxtrans.embedded.QueryResult;

import org.python.google.common.annotations.VisibleForTesting;
import org.python.google.common.base.Function;
import org.python.google.common.base.Throwables;
import org.python.google.common.collect.Iterables;
import org.python.google.common.collect.Lists;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class CsvWriter extends AbstractOutputWriter implements OutputWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ICsvListWriter csv;

    @VisibleForTesting
    String outputFilePath;

    @VisibleForTesting
    String[] header;

    private boolean firstWrite = true;

    @Override
    public void start() {
        Writer fileWriter;
        try {
            File outputFile = createOutputFile();
            fileWriter = new FileWriter(outputFile);
            logger.debug("Started CSV output writer, writing to file: {}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error opening file located at {}", outputFilePath);
            throw Throwables.propagate(e);
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

            for (String epoch : splitResults.keySet()) {
                results = splitResults.get(epoch);

                if (firstWrite) {
                    writeHeader(results);
                }
                writeBody(results, epoch);
            }
        } catch (IOException e) {
            logger.error("Error writing header to file located at {}", outputFilePath);
            throw Throwables.propagate(e);
        }
    }

    private void writeHeader(Iterable<QueryResult> results) throws IOException {
        header = createHeader(results);

        try {
            csv.writeHeader(header);
        } finally {
            csv.flush();
        }
        firstWrite = false;
    }

    private void writeBody(Iterable<QueryResult> results, String epoch) throws IOException {
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
        SortedMap<String, List<QueryResult>> resultsByTime = Maps.newTreeMap();

        for (QueryResult result : results) {
            String epoch = String.valueOf(result.getEpoch(TimeUnit.SECONDS));

            if (resultsByTime.containsKey(epoch)) {
                List<QueryResult> current = resultsByTime.get(epoch);
                current.add(result);
                resultsByTime.put(epoch, current);
            } else {
                resultsByTime.put(epoch, Lists.newArrayList(result));
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

    private String[] createHeader(Iterable<QueryResult> results) {
        List<String> headerList = Lists.newArrayList(
                Iterables.transform(results, queryResultToHeader()));

        headerList.add(0, "time");

        return headerList.toArray(new String[headerList.size()]);
    }

    /**
     * We have no guarantee that the results will always be in the same order,
     * so we make sure to align them according to the header on each query.
     */
    @VisibleForTesting
    List<Object> alignResults(Iterable<QueryResult> results, String epoch) {
        Object[] alignedResults = new Object[Iterables.size(results) + 1];
        List<String> headerList = Lists.newArrayList(header);

        alignedResults[0] = epoch;

        for (QueryResult result : results) {
            alignedResults[headerList.indexOf(result.getName())] = result.getValue();
        }

        return Lists.newArrayList(alignedResults);
    }

    private Function<QueryResult, String> queryResultToHeader() {
        return new Function<QueryResult, String>() {
            @Override
            public String apply(QueryResult queryResult) {
                return queryResult.getName();
            }
        };
    }
}