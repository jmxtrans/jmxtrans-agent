/*
 * Copyright (c) 2010-2016 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.jmxtrans.agent.kafka;

import info.batey.kafka.unit.KafkaUnitRule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Stewart Henderson
 */
public class KafkaOutputWriterTest {

    @Rule
    public KafkaUnitRule kafkaUnitRule = new KafkaUnitRule(5000, 5001);

    @Test
    public void simpleRequest() throws Exception {
        String topicName = "test";
        KafkaOutputWriter writer = new KafkaOutputWriter();
        final Map<String, String> configuration = createConfiguration(topicName);
        writer.postConstruct(configuration);
        writer.setProducer(createProducer(configuration));
        writer.writeQueryResult("foo", null, 1);
        writer.postCollect();
        List<String> messages = kafkaUnitRule.getKafkaUnit().readMessages(topicName, 1);
        assertEquals(1, messages.size());
    }

    @Test
    public void manyMetrics() throws Exception {
        String topicName = "test";
        KafkaOutputWriter writer = new KafkaOutputWriter();
        final Map<String, String> configuration = createConfiguration(topicName);
        writer.postConstruct(configuration);
        writer.setProducer(createProducer(configuration));
        writer.writeQueryResult("foo", null, 1);
        writer.writeQueryResult("foo2", null, 2.0);
        writer.postCollect();
        List<String> messages = kafkaUnitRule.getKafkaUnit().readMessages(topicName, 2);
        assertEquals(2, messages.size());
    }

    private Map<String, String> createConfiguration(final String topicName) {
        Map<String, String> s = new HashMap<>();
        s.put("bootstrap.servers", kafkaUnitRule.getKafkaUnit().getKafkaConnect());
        s.put("zk.connect", String.valueOf(kafkaUnitRule.getKafkaUnit().getZkPort()));
        s.put("key.serializer", StringSerializer.class.getCanonicalName());
        s.put("value.serializer", StringSerializer.class.getCanonicalName());
        s.put("topic", topicName);
        return s;
    }

    private KafkaProducer<String, String> createProducer(Map<String, String> configuration) {
        Properties properties = new Properties();
        properties.putAll(configuration);
        return new KafkaProducer<>(properties);
    }
}
