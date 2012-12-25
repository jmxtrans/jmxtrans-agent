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
     * No-op implementation
     */
    @Override
    public void start() {
    }

    /**
     * No-op implementation
     */
    @Override
    public void stop() throws Exception {
    }

    /**
     * Convert value of this setting to a Java <b>int</b>.
     * <p/>
     * If the setting is not found or is not an int, an exception is thrown.
     *
     * @param name name of the setting / property
     * @return int value of the setting / property
     * @throws IllegalArgumentException if setting is not found or is not an integer.
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
     * Convert value of this setting to a Java <b>int</b>.
     * <p/>
     * If the property is not found, the <code>defaultValue</code> is returned. If the property is not an int, an exception is thrown.
     *
     * @param name         name of the property
     * @param defaultValue default value if the property is not defined.
     * @return int value of the property or <code>defaultValue</code> if the property is not defined.
     * @throws IllegalArgumentException if setting is not is not an integer.
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
     * Convert value of this setting to a Java <b>long</b>.
     * <p/>
     * If the property is not found, the <code>defaultValue</code> is returned. If the property is not a long, an exception is thrown.
     *
     * @param name         name of the property
     * @param defaultValue default value if the property is not defined.
     * @return int value of the property or <code>defaultValue</code> if the property is not defined.
     * @throws IllegalArgumentException if setting is not is not a long.
     */
    protected long getLongSetting(String name, long defaultValue) throws IllegalArgumentException {
        if (settings.containsKey(name)) {

            String value = settings.get(name).toString();
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Setting '" + name + "=" + value + "' is not a long on " + this.toString());
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Convert value of this setting to a Java <b>boolean</b> (via {@link Boolean#parseBoolean(String)}).
     * <p/>
     * If the property is not found, the <code>defaultValue</code> is returned.
     *
     * @param name         name of the property
     * @param defaultValue default value if the property is not defined.
     * @return int value of the property or <code>defaultValue</code> if the property is not defined.
     */
    protected boolean getBooleanSetting(String name, boolean defaultValue) {
        if (settings.containsKey(name)) {

            String value = settings.get(name).toString();
            return Boolean.parseBoolean(value);

        } else {
            return defaultValue;
        }
    }

    /**
     * Convert value of this setting to a Java <b>int</b>.
     * <p/>
     * If the setting is not found, an exception is thrown.
     *
     * @param name name of the property
     * @return value of the property
     * @throws IllegalArgumentException if setting is not found.
     */
    protected String getStringSetting(String name) throws IllegalArgumentException {
        if (!settings.containsKey(name)) {
            throw new IllegalArgumentException("No setting '" + name + "' found");
        }
        return settings.get(name).toString();
    }

    /**
     * Return the value of the given property.
     * <p/>
     * If the property is not found, the <code>defaultValue</code> is returned.
     *
     * @param name         name of the property
     * @param defaultValue default value if the property is not defined.
     * @return value of the property or <code>defaultValue</code> if the property is not defined.
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


}
