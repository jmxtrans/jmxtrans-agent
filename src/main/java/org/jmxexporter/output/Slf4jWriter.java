/*
 * Copyright 2008-2012 Xebia and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmxexporter.output;

import org.jmxexporter.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="http://www.slf4j.org/">SLF4J</a> based {@linkplain OutputWriter} implementation.
 * <p/>
 * Settings:
 * <ul>
 * <li>"logger": Name of the logger. Optional, default value: "<code>org.jmxexporter.output.Slf4jWriter</code>"</li>
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
