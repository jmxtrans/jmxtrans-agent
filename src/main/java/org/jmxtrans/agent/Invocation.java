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

import org.jmxtrans.agent.util.Preconditions2;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Invocation {

    @Nullable
    protected final ObjectName objectName;
    @Nonnull
    protected final String operationName;
    @Nullable
    protected final String resultAlias;
    @Nonnull
    protected final Object[] params;
    @Nonnull
    protected final String[] signature;
    private final Logger logger = Logger.getLogger(getClass().getName());

    public Invocation(@Nullable String objectName, @Nonnull String operationName, @Nonnull Object[] params, @Nonnull String[] signature, @Nullable String resultAlias) {
        try {
            this.objectName = objectName == null ? null : new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid objectName '" + objectName + "'", e);
        }
        this.operationName = Preconditions2.checkNotNull(operationName, "operationName");
        this.params = Preconditions2.checkNotNull(params, "params");
        this.signature = Preconditions2.checkNotNull(signature, "signature");
        this.resultAlias = resultAlias;
    }

    public void invoke(MBeanServer mbeanServer, OutputWriter outputWriter) {
        Set<ObjectName> objectNames = mbeanServer.queryNames(objectName, null);
        for (ObjectName on : objectNames) {
            try {
                Object result = mbeanServer.invoke(on, operationName, params, signature);
                outputWriter.writeInvocationResult(resultAlias, result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception invoking " + on + "#" + operationName + "(" + Arrays.toString(params) + ")", e);
            }
        }
    }

    @Override
    public String toString() {
        return "Invocation{" +
                "objectName=" + objectName +
                ", operationName='" + operationName + '\'' +
                ", resultAlias='" + resultAlias + '\'' +
                ", params=" + Arrays.toString(params) +
                ", signature=" + Arrays.toString(signature) +
                '}';
    }
}
