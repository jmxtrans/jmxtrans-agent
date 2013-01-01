/*
 * Copyright 1012-2013 the original author or authors.
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
package org.jmxexporter;

/**
 * Generic exception of the JMX Exporter component.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class JmxExporterException extends RuntimeException {
    public JmxExporterException() {
        super();
    }

    public JmxExporterException(String message) {
        super(message);
    }

    public JmxExporterException(String message, Throwable cause) {
        super(message, cause);
    }

    public JmxExporterException(Throwable cause) {
        super(cause);
    }
}
