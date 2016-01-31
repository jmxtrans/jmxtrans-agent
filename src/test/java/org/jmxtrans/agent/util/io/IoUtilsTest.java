package org.jmxtrans.agent.util.io;

import org.jmxtrans.agent.FileOverwriterOutputWriter;
import org.jmxtrans.agent.RollingFileOutputWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

/**
 * Created by cleclerc on 31/01/16.
 */
public class IoUtilsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();


    @Test
    public void getFileLastModificationDateFromClasspath() throws Exception {

        long lastModificationDate = IoUtils.getFileLastModificationDate("classpath:org/jmxtrans/agent/util/io/a.xml");
        assertThat(lastModificationDate, not(is(0L)));
    }

    @Test
    public void getFileLastModificationDateFromFile() throws Exception {

        long before = System.currentTimeMillis();

        File xmlFile = tmp.newFile("b.xml");

        FileOutputStream out = new FileOutputStream(xmlFile);

        out.write("<parent></parent>".getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();


        String filePath = xmlFile.toString();

        {
            long lastModificationDate = IoUtils.getFileLastModificationDate(filePath);
            System.out.println(filePath + "\t" + new Timestamp(lastModificationDate) + "\t" + lastModificationDate);
            // the precision of last modified may not very good, take a margin of 1sec
            assertThat((lastModificationDate - before) + "ms", Math.abs(lastModificationDate - before), lessThan(1000L));
        }
        {
            String fileUrl = "file://" + filePath;
            long lastModificationDate = IoUtils.getFileLastModificationDate(fileUrl);
            System.out.println(filePath + "\t" + new Timestamp(lastModificationDate) + "\t" + lastModificationDate);
            // the precision of last modified may not very good, take a margin of 1sec
            assertThat((lastModificationDate - before) + "ms", Math.abs(lastModificationDate - before), lessThan(1000L));
        }
    }

    @Test
    public void getFileAsDocumentFromFile() throws Exception {
        Path xmlFile = tmp.newFile("b.xml").toPath();

        Files.write(xmlFile, "<parent></parent>".getBytes(StandardCharsets.UTF_8));

        String filePath = xmlFile.toString();

        {
            System.out.println(filePath);
            Document document = IoUtils.getFileAsDocument(filePath);
            System.out.println(document.getDocumentElement());
            assertThat(document, notNullValue());
        }
        {
            String fileUrl = "file://" + filePath;
            System.out.println(fileUrl);
            Document document = IoUtils.getFileAsDocument(fileUrl);
            System.out.println(document.getDocumentElement());
            assertThat(document, notNullValue());
        }
    }

    @Test
    public void getFileAsDocumentFromClasspath() throws Exception {
        Document document = IoUtils.getFileAsDocument("classpath:org/jmxtrans/agent/util/io/a.xml");
        assertThat(document, notNullValue());
    }
}