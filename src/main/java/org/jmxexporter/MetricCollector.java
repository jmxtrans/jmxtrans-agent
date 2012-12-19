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
package org.jmxexporter;

import javax.management.Attribute;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class MetricCollector {


    protected Result getResult(long epoch, Attribute attribute) {
        Object value = attribute.getValue();
        if (value instanceof CompositeDataSupport) {
            // getResult(resList, info, oi, attribute.getName(), (CompositeData) value, query);
        } else if (value instanceof CompositeData[]) {
            for (CompositeData cd : (CompositeData[]) value) {
                // getResult(resList, info, oi, attribute.getName(), cd, query);
            }
        } else if (value instanceof ObjectName[]) {
            /* Result r = getNewResultObject(info, oi, attribute.getName(), query);
            for (ObjectName obj : (ObjectName[]) value) {
                r.addValue(obj.getCanonicalName(), obj.getKeyPropertyListString());
            }
            resList.add(r);
            */
        } else if (value.getClass().isArray()) {
            return new Result(attribute.getName(), value, epoch);
            /*
            // OMFG: this is nutty. some of the items in the array can be
            // primitive! great interview question!
            Result r = getNewResultObject(info, oi, attribute.getName(), query);
            for (int i = 0; i < Array.getLength(value); i++) {
                Object val = Array.get(value, i);
                r.addValue(attribute.getName() + "." + i, val);
            }
            resList.add(r);
            */
        } else if (value instanceof TabularDataSupport) {
            TabularDataSupport tabularValue = (TabularDataSupport) value;
            return getResultForTabularDataSupport(tabularValue);
        } else {
            return new Result(attribute.getName(), value, epoch);
        }
        return null;
    }

    private Result getResultForTabularDataSupport(TabularDataSupport tabularValue) {
        System.out.println(tabularValue);
        return null;
        /*
    TabularDataSupport tds = (TabularDataSupport) value;
    Result r = getNewResultObject(info, oi, attribute.getName(), query);
    processTabularDataSupport(resList, info, oi, r, attribute.getName(), tds, query);
    resList.add(r);
    */
    }

}
