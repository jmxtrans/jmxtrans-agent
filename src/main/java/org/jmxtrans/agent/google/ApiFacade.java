/*
 * Copyright (c) 2010-2013 the original author or authors
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
package org.jmxtrans.agent.google;

import org.jmxtrans.agent.util.json.Json;
import org.jmxtrans.agent.util.json.JsonArray;
import org.jmxtrans.agent.util.json.JsonObject;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author <a href="mailto:eminkevich@scentregroup.com">Evgeny Minkevich</a>
 * @author <a href="mailto:msimspon@scentregroup.com">Mitch Simpson</a>
 */
public class ApiFacade {

    private static final Logger logger = Logger.getLogger(ApiFacade.class.getName());
    private static String projectId = null;

    private static Connection cf;

    static Boolean initConnection(String project, String account, String accountKey, String credentialsFile) {
        projectId = project;
        try {
            cf = new Connection(account, accountKey, credentialsFile);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to initialise metric service connection", e);
            return false;
        }
        return true;
    }

    static void uploadMetric(String metricName, String start, String end, Object value, Map<String, String> labelValues) {

        String request = "projects/" + projectId + "/timeSeries";

        JsonObject root = new JsonObject();
        JsonArray timeSeries = new JsonArray();
        root.add("timeSeries", timeSeries);

        JsonObject series0 = new JsonObject();
        timeSeries.add(series0);

        series0.add("resource", new JsonObject().add("type", "global"));

        JsonObject metric = new JsonObject();
        metric.add("type", "custom.googleapis.com/" + metricName);

        JsonObject labels = new JsonObject();
        for (Map.Entry<String, String> labelEntry : labelValues.entrySet()) {
            labels.add(labelEntry.getKey(), labelEntry.getValue());
        }

        metric.add("labels", labels);
        series0.add("metric", metric);

        JsonArray points = new JsonArray();
        points.add(getPoint(start, end, value));

        series0.add("points", points);

        try {
            cf.doPost(request, root.toString());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to upload metric value for : " + metricName + " : " + value, e);
        }
    }

    private static JsonObject getPoint(String start, String end, Object value) {
        JsonObject point = new JsonObject();

        JsonObject interval = new JsonObject();
        interval.add("startTime", start);
        interval.add("endTime", end);
        point.add("interval", interval);

        JsonObject val = new JsonObject();

        if (value instanceof Double || value instanceof Float) {
            val.add("doubleValue", (Double) value);
        } else if (value instanceof Long || value instanceof Integer) {
            val.add("int64Value", "" + value);
        } else if (value instanceof Boolean) {
            val.add("boolValue", (Boolean) value);
        } else if (value instanceof String) {
            val.add("stringValue", (String) value);
        } else {
            val.add("stringValue", value.toString());
        }

        point.add("value", val);

        return point;
    }

    static String initCustomMetricDescriptor(
            @Nonnull String metricType,
            @Nonnull String metricKind,
            @Nonnull String metricUnit,
            @Nonnull String valueType,
            @Nonnull List<String> classificationLabels
    ) {
        try {
            String customMetricType = "custom.googleapis.com/" + metricType;

            if (checkMetricDescriptorExists(projectId, customMetricType)) {
                logger.log(Level.INFO, "Existing custom metric found : " + customMetricType);
                return metricKind;
            }

            createCustomMetricDescriptor(
                    metricType,
                    metricKind,
                    metricUnit,
                    valueType,
                    classificationLabels);

            return metricKind;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to initialise metric : " + metricType, e);
            return "FAIL";
        }
    }

    private static boolean checkMetricDescriptorExists(String project, String metricName) {
        String request = "projects/" + project + "/metricDescriptors/" + metricName;
        try {
            String response = cf.doGet(request, null);
            logger.fine("Metric received : " + response);
            JsonObject obj = Json.parse(response).asObject();
            if (obj != null &&
                    obj.get("name") != null &&
                    obj.get("name").asString().equalsIgnoreCase(request)) {
                return true;
            }
        } catch (RuntimeException e) {
            JsonObject error = Json.parse(e.getMessage()).asObject();
            if (error.get("error")!=null && error.get("error").asObject().get("code").asInt() == 404){
                logger.info("Metric '" + metricName + "' is not found in the project");
            } else {
                logger.log(Level.WARNING, "Unable to retrieve metric : " + e.getMessage(), e);
                logger.log(Level.WARNING, "Request : " + request);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve metric : " + e.getMessage(), e);
            logger.log(Level.WARNING, "Request : " + request);
        }
        return false;
    }

    /**
     * This method creates a custom metric with arbitrary names, description,
     * and units.
     */
    private static void createCustomMetricDescriptor(
            @Nonnull String metricName,
            @Nonnull String metricKind,
            @Nonnull String metricUnit,
            @Nonnull String metricValueType,
            @Nonnull List<String> classificationLabels
    ) throws Exception {

        logger.info("Registering new custom metric : " + metricName + " : " + metricKind + " : " + metricUnit + " : " + metricValueType + " : " + classificationLabels);

        String url = "projects/" + projectId + "/metricDescriptors";
        String fullName = "custom.googleapis.com/" + metricName;

        JsonObject md = new JsonObject();
        md.add("name", url + "/" + URLEncoder.encode(fullName, "UTF-8"));
        md.add("type", fullName);

        md.add("metricKind", metricKind.toUpperCase());
        md.add("valueType", metricValueType.toUpperCase());
        md.add("unit", metricUnit);

        // Metric Labels
        JsonArray labels = new JsonArray();
        for (String entry : classificationLabels) {
            JsonObject label = new JsonObject();
            label.add("key", entry);
            label.add("description", entry);
            label.add("valueType", "STRING");
            labels.add(label);
        }
        md.add("labels", labels);

        String request = md.toString();
        logger.fine("Metric creation request : " + request);

        String response = cf.doPost(url, md.toString());
        logger.finer("Metric creation response" + response);

        logger.info("Created custom metric : " + metricName);
    }
}
