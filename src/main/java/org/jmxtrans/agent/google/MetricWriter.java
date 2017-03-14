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

import org.jmxtrans.agent.util.StringUtils2;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * @author <a href="mailto:eminkevich@scentregroup.com">Evgeny Minkevich</a>
 * @author <a href="mailto:msimspon@scentregroup.com">Mitch Simpson</a>
 */
public class MetricWriter {

    private static final String[] SET_VALUES =
            new String[]{"projectid", "serviceaccount", "serviceaccountkey", "applicationcredentials", "separator", "nameprefix", "hostname"};
    private static final Set<String> RESERVED_KEYWORDS = new HashSet<String>(Arrays.asList(SET_VALUES));

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private String separator;
    private String namePrefix = "";

    private Map<String, String> staticLabels = new LinkedHashMap<>();

    private Map<String, String> metricDescriptors = new HashMap<>();
    private String cumulativePeriodStart;

    static MetricWriter getMetricWriter(@Nonnull Map<String, String> settings) {
        MetricWriter writer = new MetricWriter();
        if (writer.init(settings))
            return writer;
        return null;
    }

    private boolean init(@Nonnull Map<String, String> settings) {

        rfc3339.setTimeZone(TimeZone.getTimeZone("UTC"));
        cumulativePeriodStart = getNow();

        String projectId = StringUtils2.trimToEmpty(settings.get("projectId"));
        if (StringUtils2.isNullOrEmpty(projectId)) {
            logger.info("Metrics Project ID is not set. Attempting to source project id from GKE.");
            projectId = getGoogleProjectId();
        }

        if (StringUtils2.isNullOrEmpty(projectId)) {
            logger.log(Level.WARNING, "Metrics Project ID is not set. Metric Collection agent stopped.");
            return false;
        }
        logger.info("Metrics GCP Project : " + projectId);

        String applicationCredentials = StringUtils2.trimToEmpty(settings.get("applicationCredentials"));
        String serviceAccount = StringUtils2.trimToEmpty(settings.get("serviceAccount"));
        String serviceAccountKey = StringUtils2.trimToEmpty(settings.get("serviceAccountKey"));

        String separator = StringUtils2.trimToEmpty(settings.get("separator"));
        this.separator = StringUtils2.isNullOrEmpty(separator) ? ":" : separator;

        String namePrefix = StringUtils2.trimToEmpty(settings.get("namePrefix"));
        this.namePrefix = StringUtils2.isNullOrEmpty(namePrefix) ? "" : namePrefix;

        initStaticLabels(settings);

        return ApiFacade.initConnection(projectId, serviceAccount, serviceAccountKey, applicationCredentials);
    }

    private String getNow() {
        return rfc3339.format(new Date());
    }

    private void initStaticLabels(Map<String, String> settings) {

        String hostname = StringUtils2.trimToEmpty(settings.get("hostname"));

        if (StringUtils2.isNullOrEmpty(hostname)) {
            hostname = System.getenv("HOSTNAME");
        }
        logger.info("Metrics HOSTNAME : " + hostname);

        this.staticLabels.put("hostname", hostname);

        for (Map.Entry<String,String> entry : settings.entrySet()) {
            String value = StringUtils2.trimToEmpty(settings.get(entry.getKey()));
            if (StringUtils2.isNullOrEmpty(value) || RESERVED_KEYWORDS.contains(entry.getKey().toLowerCase().trim())) {
                continue;
            }
            this.staticLabels.put(entry.getKey(), value);
            logger.log(Level.INFO, "Static label : " + entry.getKey() + " : " + value);
        }
    }

    private String getGoogleProjectId() {
        BufferedReader in = null;
        try {
            URLConnection yc = new URL("http://metadata.google.internal/computeMetadata/v1/project/project-id")
                    .openConnection();
            yc.setRequestProperty("Metadata-Flavor", "Google");
            in = new BufferedReader(new InputStreamReader(yc.getInputStream(), "UTF-8"));
            return in.readLine();
        } catch (Exception e) {
            logger.log(Level.INFO, "Failed to source project id from GCP. Do we run in GCP? ", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    public void writeQueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value) {

        LinkedHashMap<String, String> classificationLabels = new LinkedHashMap<>();

        classificationLabels.putAll(Collections.unmodifiableMap(staticLabels));

        String[] nameParts = name.split(this.separator);
        String metricName = nameParts[0];
        for (int i = 1; i < nameParts.length; i++) {
            classificationLabels.put("attribute_" + i, nameParts[i]);
        }

        String metricKind = metricDescriptors.get(metricName);
        if (null == metricKind) {
            metricKind = initMetricDescriptor(metricName, type, value, classificationLabels);
            metricDescriptors.put(metricName, metricKind);
        }

        if (!metricKind.equalsIgnoreCase("FAIL")) {
            writeValue(metricName, value, classificationLabels);
        }
    }

    private void writeValue(@Nonnull String metricName, @Nonnull Object value, @Nonnull Map<String, String> classificationLabels) {
        String fullMetricName = this.namePrefix + metricName;
        String endTimeString = getNow();

        logger.log(Level.FINER, fullMetricName + " : " + endTimeString + " : " + value.toString() + " : " + classificationLabels.toString());

        if (metricDescriptors.get(metricName).equalsIgnoreCase("GAUGE")) {
            ApiFacade.uploadMetric(fullMetricName, endTimeString, endTimeString, value, classificationLabels);
        }

        if (metricDescriptors.get(metricName).equalsIgnoreCase("CUMULATIVE")) {
            ApiFacade.uploadMetric(fullMetricName, cumulativePeriodStart, endTimeString, value, classificationLabels);
        }
    }

    private String initMetricDescriptor(String metricName, String type, Object value, HashMap<String, String> classificationLabels) {

        ArrayList<String> labels = new ArrayList<>();
        String fullMetricName = this.namePrefix + metricName;

        String metricKind;
        String metricUnit;
        String valueType = null;

        // Selecting metric kind, metric unit and value type. Format (assuming ':' is a separator):
        // [ KIND [: UNIT [: VALUE TYPE ]]]
        if (!StringUtils2.isNullOrEmpty(type)) {
            String[] typeParts = type.split(this.separator);
            metricKind = typeParts[0];
            if (typeParts.length > 1) {
                metricUnit = typeParts[1];
            } else {
                metricUnit = "1";
            }
            if (typeParts.length > 2) {
                valueType = typeParts[2];
            }
        } else {
            metricKind = "GAUGE";
            metricUnit = "1";
        }

        if (null == valueType) {
            valueType = selectValueType(value);
        }

        for (Map.Entry<String, String> entry : classificationLabels.entrySet()) {
            String key = entry.getKey();
            labels.add(key);
        }

        return ApiFacade.initCustomMetricDescriptor(
                fullMetricName,
                metricKind,
                metricUnit,
                valueType,
                labels);
    }

    private static String selectValueType(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return "DOUBLE";
        }
        if (value instanceof Long || value instanceof Integer) {
            return "INT64";
        }
        if (value instanceof Boolean) {
            return "BOOL";
        }
        return "STRING";
    }
}
