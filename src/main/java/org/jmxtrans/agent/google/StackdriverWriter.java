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

import org.jmxtrans.agent.AbstractOutputWriter;
import org.jmxtrans.agent.OutputWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:eminkevich@scentregroup.com">Evgeny Minkevich</a>
 * @author <a href="mailto:msimspon@scentregroup.com">Mitch Simpson</a>
 */
public class StackdriverWriter extends AbstractOutputWriter implements OutputWriter {

    private MetricWriter writer;

    @Override
    public void postConstruct(@Nonnull Map<String, String> settings) {
        writer = MetricWriter.getMetricWriter(settings);
    }

    @Override
    public void writeQueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value) {
        if (null!=writer)
            writer.writeQueryResult(name,type,value);
    }

    @Override
    public void writeInvocationResult(@Nonnull String invocationName, @Nullable Object value) throws IOException {
        if (null!=writer)
            writer.writeQueryResult(invocationName,"GAUGE:1",value);
    }
}
