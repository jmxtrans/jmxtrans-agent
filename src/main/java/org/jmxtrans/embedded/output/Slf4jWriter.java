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
package org.jmxtrans.embedded.output;

import org.jmxtrans.embedded.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="http://www.slf4j.org/">SLF4J</a> based {@linkplain OutputWriter} implementation.
 * <p/>
 * Settings:
 * <ul>
 * <li>"logger": Name of the logger. Optional, default value: "<code>org.jmxtrans.embedded.output.Slf4jWriter</code>"</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class Slf4jWriter extends AbstractOutputWriter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Initialize the logger.
     */
    @Override
    public void start() {
        super.start();
        logger = LoggerFactory.getLogger(getStringSetting("logger", getClass().getName()));
    }

    @Override
    public void write(Iterable<QueryResult> results) {
        for (QueryResult result : results) {
            logger.info(result.getName() + "\t" + result.getEpochInMillis() + "\t" + result.getValue());
        }
    }
}
