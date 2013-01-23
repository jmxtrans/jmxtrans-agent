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

import java.io.IOException;
import java.io.Writer;

/**
 * Used for testing. Send results to "<code>/dev/null</code>".
 * <p/>
 * Settings:
 * <ul>
 * <li>"enabled": flag to enable/disable the writer. Optional, default value: <code>true</code>.</li>
 * </ul>
 * <p/>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class NoOpWriter extends AbstractOutputWriter {

    static class NullWriter extends Writer {

        @Override
        public void write(char[] buffer, int off, int len) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    private Writer out = new NullWriter();

    /**
     * Write metrics to <code>/dev/null</code>
     */
    @Override
    public void write(Iterable<QueryResult> results) {
        for (QueryResult result : results) {
            try {
                out.write(result.toString());
            } catch (IOException e) {
                // never occurs
            }
        }
    }
}
