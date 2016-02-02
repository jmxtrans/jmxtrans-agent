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

import org.jmxtrans.agent.util.collect.EvictingQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class PerMinuteSummarizerOutputWriter extends AbstractOutputWriter implements OutputWriter {

    protected OutputWriter delegate;
    protected Map<String, Queue<QueryResult>> previousQueryResultsByMetricName = new HashMap<>();

    public PerMinuteSummarizerOutputWriter() {
    }

    public PerMinuteSummarizerOutputWriter(OutputWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException {
        delegate.writeInvocationResult(invocationName, value);
    }

    @Override
    public void writeQueryResult(@Nonnull String metricName, @Nullable String metricType, @Nullable Object value) throws IOException {

        QueryResult currentResult = new QueryResult(metricName, metricType, value, System.currentTimeMillis());

        if ("counter".equals(currentResult.getType())) {

            QueryResult previousResult = getPreviousQueryResult(currentResult);

            storeQueryResult(currentResult);
            QueryResult newCurrentResult = perMinute(currentResult, previousResult);
            if (logger.isLoggable(getDebugLevel()))
                logger.log(getDebugLevel(), "Metric " + currentResult.getName() + " is a counter " +
                        "current=" + currentResult + ", " +
                        "previous=" + previousResult + ", " +
                        "newCurrent.value=" + newCurrentResult.getValue());

            delegate.writeQueryResult(newCurrentResult.getName(), newCurrentResult.getType(), newCurrentResult.getValue());

        } else {
            if (logger.isLoggable(getTraceLevel()))
                logger.log(getTraceLevel(), "Metric " + currentResult.getName() + " is a NOT a counter");
            delegate.writeQueryResult(metricName, metricType, value);
        }
    }

    protected void storeQueryResult(@Nullable QueryResult currentResult) {
        if (currentResult == null)
            return;

        Queue<QueryResult> queue = previousQueryResultsByMetricName.get(currentResult.getName());
        if (queue == null) {
            queue = new EvictingQueue<>(3);
            previousQueryResultsByMetricName.put(currentResult.getName(), queue);
        }
        queue.add(currentResult);
    }

    /**
     */
    @Nullable
    protected QueryResult getPreviousQueryResult(@Nonnull QueryResult currentResult) {
        Queue<QueryResult> queue = previousQueryResultsByMetricName.get(currentResult.getName());
        if (queue == null) {
            return null;
        }

        final long targetTimeInMillis = currentResult.getEpochInMillis() - TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS);
        long closestDistanceToTarget = Long.MAX_VALUE;
        QueryResult closestQueryResultToTarget = null;
        for (QueryResult queryResult : queue) {
            if (queryResult.isValueGreaterThan(currentResult)) {
                // skip older result that is greater than current value
                // ever increasing counter must be increasing
            } else {
                long distanceToTarget = Math.abs(queryResult.getEpochInMillis() - targetTimeInMillis);
                if (distanceToTarget < closestDistanceToTarget) {
                    closestQueryResultToTarget = queryResult;
                    closestDistanceToTarget = distanceToTarget;
                }
            }
        }

        return closestQueryResultToTarget;
    }

    @Nonnull
    public QueryResult perMinute(@Nonnull QueryResult currentResult, @Nullable QueryResult previousResult) {

        if (!(currentResult.getValue() instanceof Number)) {
            if (logger.isLoggable(getInfoLevel()))
                logger.log(getInfoLevel(), "Current value is not a number, cannot calculate derivative " + currentResult);

            return currentResult;
        }

        if (previousResult == null) {
            if (logger.isLoggable(getTraceLevel()))
                logger.log(getTraceLevel(), "No previous value found for metric '" + currentResult.getName() + "'");

            return new QueryResult(currentResult.getName(), "gauge", currentResult.getValue(), currentResult.getEpochInMillis());
        }
        if (!(previousResult.getValue() instanceof Number)) {
            if (logger.isLoggable(getInfoLevel()))
                logger.log(getInfoLevel(), "previous value is not a number, cannot calculate derivative " + previousResult);

            return currentResult;
        }

        BigDecimal durationInMillis = new BigDecimal(currentResult.getEpochInMillis() - previousResult.getEpochInMillis());

        Number currentValue = (Number) currentResult.getValue();
        Number previousValue = (Number) previousResult.getValue();

        if (((Comparable) currentValue).compareTo(previousValue) < 0) {
            if (logger.isLoggable(getTraceLevel()))
                logger.log(getTraceLevel(), "Previous value is greater than current value for metric '" + currentResult.getName() + "', ignore it");

            return new QueryResult(currentResult.getName(), "gauge", currentResult.getValue(), currentResult.getEpochInMillis());
        }

        BigDecimal valueDelta;
        if (currentValue instanceof Long) {
            valueDelta = new BigDecimal(currentValue.longValue() - previousValue.longValue());
        } else if (currentValue instanceof Integer) {
            valueDelta = new BigDecimal(currentValue.intValue() - previousValue.intValue());
        } else if (currentValue instanceof Float) {
            valueDelta = new BigDecimal(currentValue.floatValue() - previousValue.floatValue());
        } else if (currentValue instanceof Double) {
            valueDelta = new BigDecimal(currentValue.doubleValue() - previousValue.doubleValue());
        } else {
            if (logger.isLoggable(getInfoLevel()))
                logger.log(getInfoLevel(), "unsupported value type '" + currentValue.getClass() + ", cannot calculate perMinute " + currentResult);

            return currentResult;
        }

        // multiply by 1000 because duration will be in millis
        // multiply by 60 for per-minute
        BigDecimal perMinute = valueDelta.movePointRight(3).multiply(new BigDecimal(60)).divide(durationInMillis, RoundingMode.HALF_UP);

        Number newCurrentValue;
        if (currentValue instanceof Long) {
            newCurrentValue = perMinute.longValue();
        } else if (currentValue instanceof Integer) {
            newCurrentValue = perMinute.intValue();
        } else if (currentValue instanceof Float) {
            newCurrentValue = perMinute.floatValue();
        } else if (currentValue instanceof Double) {
            newCurrentValue = perMinute.doubleValue();
        } else {
            if (logger.isLoggable(getInfoLevel()))
                logger.log(getInfoLevel(), "Illegal state " + previousResult);

            return currentResult;
        }

        return new QueryResult(currentResult.getName(), "gauge", newCurrentValue, currentResult.getEpochInMillis());
    }

    @Override
    public void postConstruct(@Nonnull Map<String, String> settings) {
        super.postConstruct(settings);
        delegate.postConstruct(settings);
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
        delegate.preDestroy();
    }

    @Override
    public void postCollect() throws IOException {
        super.postCollect();
        delegate.postCollect();
    }

    @Override
    public void preCollect() throws IOException {
        super.preCollect();
        delegate.preCollect();
    }

    public void setDelegate(OutputWriter delegate) {
        this.delegate = delegate;
    }
}
