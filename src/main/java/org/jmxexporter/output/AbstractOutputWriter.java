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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public abstract class AbstractOutputWriter implements OutputWriter {

    public final static String SETTING_PORT = "port";
    public final static String SETTING_HOST = "host";
    public final static String SETTING_NAME_PREFIX = "namePrefix";

    private Map<String, Object> settings = new HashMap<String, Object>();

    /**
     * @param name
     * @return
     * @throws IllegalArgumentException no setting found
     */
    protected int getIntSetting(String name) throws IllegalArgumentException {
        String value = getStringSetting(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Setting '" + name + "=" + value + "' is not an integer on " + this.toString());
        }
    }

    /**
     * @param name
     * @return
     * @throws IllegalArgumentException no setting found
     */
    protected int getIntSetting(String name, int defaultValue) throws IllegalArgumentException {
        if (settings.containsKey(name)) {

            String value = settings.get(name).toString();
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Setting '" + name + "=" + value + "' is not an integer on " + this.toString());
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * @param name
     * @return
     * @throws IllegalArgumentException no setting found
     */
    protected String getStringSetting(String name) throws IllegalArgumentException {
        if (!settings.containsKey(name)) {
            throw new IllegalArgumentException("No setting '" + name + "' found");
        }
        return settings.get(name).toString();
    }

    /**
     * @param name
     * @return
     */
    protected String getStringSetting(String name, String defaultValue) {
        if (settings.containsKey(name)) {
            return settings.get(name).toString();
        } else {
            return defaultValue;
        }
    }

    @Override
    public abstract void write(Iterable<QueryResult> results);

    @Override
    public String toString() {
        return getClass() + "{" +
                "settings=" + settings +
                '}';
    }


    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() throws Exception {
    }


}
