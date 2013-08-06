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

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class PerMinuteSummarizerOutputWriterTest {

    PerMinuteSummarizerOutputWriter writer;

    @Before
    public void before(){
        writer = new PerMinuteSummarizerOutputWriter();
    }
    @Test
    public void testPerMinute_60s_elapse() throws Exception {
        DateTime previousTime = new DateTime(2013, 01, 01, 14, 01, 31);
        QueryResult previous = new QueryResult("metric1", 10, previousTime.getMillis());

        // small offest of 45 millis caused by metrics collection
        DateTime currentTime = previousTime.plusSeconds(60).plusMillis(45);
        QueryResult current = new QueryResult("metric1", 23, currentTime.getMillis());

        QueryResult actualResult = writer.perMinute(current, previous);
        int actualPerMinuteValue = (Integer) actualResult.getValue();

        assertThat(actualPerMinuteValue, is(13));
    }

    @Test
    public void testPerMinute_20s_elapse() throws Exception {
        DateTime previousTime = new DateTime(2013, 01, 01, 14, 01, 31);
        QueryResult previous = new QueryResult("metric1", 10, previousTime.getMillis());

        // small offest of 45 millis caused by metrics collection
        DateTime currentTime = previousTime.plusSeconds(20).plusMillis(45);
        QueryResult current = new QueryResult("metric1", 23, currentTime.getMillis());

        QueryResult actualResult = writer.perMinute(current, previous);
        int actualPerMinuteValue = (Integer) actualResult.getValue();

        assertThat(actualPerMinuteValue, is(39));
    }

    @Test
    public void testPerMinute_90s_elapse() throws Exception {
        DateTime previousTime = new DateTime(2013, 01, 01, 14, 01, 31);
        QueryResult previous = new QueryResult("metric1", 10, previousTime.getMillis());

        // small offest of 45 millis caused by metrics collection
        DateTime currentTime = previousTime.plusSeconds(90).plusMillis(45);
        QueryResult current = new QueryResult("metric1", 40, currentTime.getMillis());

        QueryResult actualResult = writer.perMinute(current, previous);
        int actualPerMinuteValue = (Integer) actualResult.getValue();

        assertThat(actualPerMinuteValue, is(20));
    }

    @Test
    public void testPerMinute_ignore_previous_value_greater_than_current() throws Exception {
        DateTime previousTime = new DateTime(2013, 01, 01, 14, 01, 31);
        QueryResult previous = new QueryResult("metric1", 40, previousTime.getMillis());

        // small offest of 45 millis caused by metrics collection
        DateTime currentTime = previousTime.plusSeconds(60).plusMillis(45);
        QueryResult current = new QueryResult("metric1", 13, currentTime.getMillis());

        QueryResult actualResult = writer.perMinute(current, previous);
        int actualPerMinuteValue = (Integer) actualResult.getValue();

        assertThat(actualPerMinuteValue, is(13));
    }

    @Test
    public void testPerMinute_null_previous_value() throws Exception {
        DateTime previousTime = new DateTime(2013, 01, 01, 14, 01, 31);
        QueryResult previous = new QueryResult("metric1", null, previousTime.getMillis());

        // small offest of 45 millis caused by metrics collection
        DateTime currentTime = previousTime.plusSeconds(60).plusMillis(45);
        QueryResult current = new QueryResult("metric1", 13, currentTime.getMillis());

        QueryResult actualResult = writer.perMinute(current, previous);
        int actualPerMinuteValue = (Integer) actualResult.getValue();

        assertThat(actualPerMinuteValue, is(13));
    }

    @Test
    public void testPerMinute_null_previous_result() throws Exception {
        DateTime previousTime = new DateTime(2013, 01, 01, 14, 01, 31);

        // small offest of 45 millis caused by metrics collection
        DateTime currentTime = previousTime.plusSeconds(60).plusMillis(45);
        QueryResult current = new QueryResult("metric1", 13, currentTime.getMillis());

        QueryResult actualResult = writer.perMinute(current, null);
        int actualPerMinuteValue = (Integer) actualResult.getValue();

        assertThat(actualPerMinuteValue, is(13));
    }

    @Test
    public void testGetPreviousResult_returns_last_result(){
        DateTime now = new DateTime();

        QueryResult expected = new QueryResult("mymetric", 10, now.minusSeconds(65).getMillis());
        writer.storeQueryResult(expected);
        writer.storeQueryResult(new QueryResult("mymetric", 20, now.minusSeconds(54).getMillis()));
        writer.storeQueryResult(new QueryResult("mymetric", 30, now.minusSeconds(34).getMillis()));

        QueryResult currentResult = new QueryResult("mymetric", 50, now.getMillis());

        QueryResult actual = writer.getPreviousQueryResult(currentResult);

        assertThat(actual,is(expected));
    }

    @Test
    public void testGetPreviousResult_returns_2nd_result(){
        DateTime now = new DateTime();

        writer.storeQueryResult(new QueryResult("mymetric", 10, now.minusSeconds(65).getMillis()));
        QueryResult expected = new QueryResult("mymetric", 20, now.minusSeconds(56).getMillis());
        writer.storeQueryResult(expected);
        writer.storeQueryResult(new QueryResult("mymetric", 30, now.minusSeconds(34).getMillis()));

        QueryResult currentResult = new QueryResult("mymetric", 50, now.getMillis());

        QueryResult actual = writer.getPreviousQueryResult(currentResult);

        assertThat(actual,is(expected));
    }

    @Test
    public void testGetPreviousResult_skip_decreasing_result(){
        DateTime now = new DateTime();

        QueryResult tooHigh = new QueryResult("mymetric", 60, now.minusSeconds(65).getMillis());
        writer.storeQueryResult(tooHigh);
        QueryResult expected = new QueryResult("mymetric", 20, now.minusSeconds(50).getMillis());
        writer.storeQueryResult(expected);
        writer.storeQueryResult(new QueryResult("mymetric", 30, now.minusSeconds(34).getMillis()));

        QueryResult currentResult = new QueryResult("mymetric", 50, now.getMillis());

        QueryResult actual = writer.getPreviousQueryResult(currentResult);

        assertThat(actual,is(expected));
    }
}
