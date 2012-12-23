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
package org.jmxexporter.util.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Inspired by <code>org.springframework.jmx.export.naming.SelfNaming</code>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public interface SelfNaming {
    /**
     * Return the <code>ObjectName</code> for the implementing object.
     */
    ObjectName getObjectName();

    /**
     * Set the <code>ObjectName</code> for the implementing object.
     */
    void setObjectName(String objectName) throws MalformedObjectNameException;

    /**
     * Set the <code>ObjectName</code> for the implementing object.
     */
    void setObjectName(ObjectName objectName);

}
