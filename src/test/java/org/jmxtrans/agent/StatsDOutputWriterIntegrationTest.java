package org.jmxtrans.agent;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class StatsDOutputWriterIntegrationTest {

    @Test
    @Ignore
    public void test() throws IOException {
        StatsDOutputWriter writer = new StatsDOutputWriter();
        Map<String, String> settings = new HashMap<>();
        settings.put(StatsDOutputWriter.SETTING_HOST, "localhost");
        settings.put(StatsDOutputWriter.SETTING_PORT, "8125");
        writer.postConstruct(settings);

        for (int measureIndex = 0; measureIndex < 10; measureIndex++) {
            for (int metricIndex = 0; metricIndex < 5; metricIndex++) {
                writer.writeQueryResult("jmxtrans-agent-test-metric-" + metricIndex, "counter", Integer.valueOf(10 * measureIndex + metricIndex));
            }
        }

    }
}
