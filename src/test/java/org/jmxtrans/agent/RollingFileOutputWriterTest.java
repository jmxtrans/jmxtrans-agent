package org.jmxtrans.agent;

import org.jmxtrans.agent.util.io.ClasspathResource;
import org.jmxtrans.agent.util.io.Resource;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.junit.Assert.assertThat;

public class RollingFileOutputWriterTest {
    @Test
    public void testWriteQueryResultMulti() throws IOException {
        Path defaultLog =Paths.get("jmxtrans-agent.data");
        Files.deleteIfExists(defaultLog);
        Resource resource = new ClasspathResource("classpath:jmxtrans-config-rolling-multi-test.xml");
        JmxTransConfigurationLoader loader = new JmxTransConfigurationXmlLoader(resource);
        JmxTransExporter exporter = new JmxTransExporter(loader);
        exporter.collectAndExport();
        String text = new String(Files.readAllBytes(defaultLog));
        String[] splitted = text.split("\n");
        assertThat(splitted, arrayContaining(containsString("jvm.thread"), containsString("os.systemLoadAverage")));
        Files.deleteIfExists(defaultLog);
    }

    @Test
    public void testWriteQueryResultSingle() throws IOException {
        Path defaultLog =Paths.get("jmxtrans-agent.data");
        Files.deleteIfExists(defaultLog);
        Resource resource = new ClasspathResource("classpath:jmxtrans-config-rolling-single-test.xml");
        JmxTransConfigurationLoader loader = new JmxTransConfigurationXmlLoader(resource);
        JmxTransExporter exporter = new JmxTransExporter(loader);
        exporter.collectAndExport();
        String text = new String(Files.readAllBytes(defaultLog));
        assertThat(text, allOf(containsString("jvm.thread="), containsString("os.systemLoadAverage=")));
        Files.deleteIfExists(defaultLog);
    }
}
