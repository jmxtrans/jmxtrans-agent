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
package org.jmxtrans.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 * @author Kristoffer Erlandsson
 */
public class JmxTransExporterConfiguration {

    /**
     * visible for test
     */
    protected List<Query> queries = new ArrayList<>();
    /**
     * visible for test
     */
    protected List<Invocation> invocations = new ArrayList<>();
    /**
     * visible for test
     */
    protected OutputWriter outputWriter = new DevNullOutputWriter();

    protected ResultNameStrategy resultNameStrategy;
    protected int collectInterval = 10;
    protected TimeUnit collectIntervalTimeUnit = TimeUnit.SECONDS;
    private int configReloadInterval = -1; // -1 == never (0 = check very often, < 10ms between checks)
    private Document document;

    /**
     * @param document
     *            The document used when creating this configuration. Will be used to detect configuration changes in
     *            the underlying configuration file. No configuration will be read from this document - configuration
     *            has to be explicitly set with the withXxx methods.
     */
    public JmxTransExporterConfiguration(Document document) {
        this.document = document;
    }

    public JmxTransExporterConfiguration withQuery(@Nonnull String objectName, @Nonnull List<String> attributes, @Nullable String resultAlias) {
        return withQuery(objectName, attributes, null, null, null, resultAlias);
    }

    public JmxTransExporterConfiguration withQuery(@Nonnull String objectName, @Nonnull List<String> attributes, @Nullable String key,
                                      @Nullable Integer position, @Nullable String type, @Nullable String resultAlias) {
        Query query = new Query(objectName, attributes, key, position, type, resultAlias, this.resultNameStrategy);
        queries.add(query);
        return this;
    }
    public JmxTransExporterConfiguration withInvocation(@Nonnull String objectName, @Nonnull String operation, @Nullable String resultAlias) {
        invocations.add(new Invocation(objectName, operation, new Object[0], new String[0], resultAlias));
        return this;
    }
    public JmxTransExporterConfiguration withOutputWriter(OutputWriter outputWriter) {
        this.outputWriter = outputWriter;
        return this;
    }

    public JmxTransExporterConfiguration withCollectInterval(int collectInterval, @Nonnull TimeUnit collectIntervalTimeUnit) {
        this.collectInterval = collectInterval;
        this.collectIntervalTimeUnit = collectIntervalTimeUnit;
        return this;
    }
    
    public JmxTransExporterConfiguration withConfigReloadInterval(int configReloadInterval) {
        if (configReloadInterval < 0) {
            throw new IllegalArgumentException("configReloadInterval must be >= 0, was: " + configReloadInterval);
        }
        this.configReloadInterval = configReloadInterval;
        return this;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public List<Invocation> getInvocations() {
        return invocations;
    }

    public OutputWriter getOutputWriter() {
        return outputWriter;
    }

    public ResultNameStrategy getResultNameStrategy() {
        return resultNameStrategy;
    }

    public int getCollectInterval() {
        return collectInterval;
    }

    public TimeUnit getCollectIntervalTimeUnit() {
        return collectIntervalTimeUnit;
    }

    @Override
    public String toString() {
        return "JmxTransExporterConfiguration{" +
                "queries=" + queries +
                ", invocations=" + invocations +
                ", outputWriter=" + outputWriter +
                ", collectInterval=" + collectInterval +
                " " + collectIntervalTimeUnit +
                '}';
    }

    public Integer getConfigReloadInterval() {
        return configReloadInterval;
    }

    public Document getDocument() {
        return document;
    }

    public void destroy() {
        getOutputWriter().preDestroy();
    }
}
