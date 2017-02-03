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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jmxtrans.agent.AbstractOutputWriter;
import org.jmxtrans.agent.util.ConfigurationUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;

public class KafkaOutputWriter extends AbstractOutputWriter {

    private KafkaProducer<String, String> producer;
    private String topic;
    private JsonFactory jsonFactory;

    @Override
    public void postConstruct(Map<String, String> settings) {
        jsonFactory = new JsonFactory();

        Properties kafkaProperties =  new Properties();
        final String bootstrapServers = ConfigurationUtils.getString(settings, "bootstrap.servers");
        final String brokerList = ConfigurationUtils.getString(settings, "metadata.broker.list");
        final String zkConnect = ConfigurationUtils.getString(settings, "zk.connect");
        final String serializerClass = ConfigurationUtils.getString(settings, "serializer.class");
        final String keySerializer = ConfigurationUtils.getString(settings, "key.serializer");
        final String valueSerializer = ConfigurationUtils.getString(settings, "value.serializer");

        topic = ConfigurationUtils.getString(settings, "topic");

        kafkaProperties.setProperty("boostrap.servers", bootstrapServers);
        kafkaProperties.setProperty("metadata.broker.list", brokerList);
        kafkaProperties.setProperty("zk.connect", zkConnect);
        kafkaProperties.setProperty("serializer.class", serializerClass);
        kafkaProperties.setProperty("key.serializer", keySerializer);
        kafkaProperties.setProperty("value.serializer", valueSerializer);

        logger.log(getInfoLevel(), "KafkaOutputWriter is configured with "
            + "metadata.broker.list=" + brokerList
            + ", zk.connect=" + zkConnect
            + ", serializer.class=" + serializerClass
        );
    }

    @Override
    public void writeInvocationResult(String invocationName, Object value) throws IOException {
    }

    @Override
    public void writeQueryResult(@Nonnull String metricName, @Nullable String metricType, @Nullable Object value) throws IOException {
        String message = createJsonMessage(metricName, metricType, value);
        producer.send(new ProducerRecord<String, String>(topic, message));
    }

    private String createJsonMessage(String metricName, final String metricType, Object value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator generator = jsonFactory.createGenerator(out, UTF8);
        generator.writeStartObject();
        generator.writeStringField("metricName", metricName);
        generator.writeStringField("metricType", metricType);
        generator.writeStringField("message", String.valueOf(value));
        generator.writeEndObject();
        generator.close();
        return out.toString("UTF-8");
    }

    @VisibleForTesting
    void setProducer(KafkaProducer<String, String> producer) {
        this.producer = producer;
    }
}