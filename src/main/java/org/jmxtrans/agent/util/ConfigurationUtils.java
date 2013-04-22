/*
 * Copyright 2010-2013, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmxtrans.agent.util;

import java.util.Map;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class ConfigurationUtils {
    private ConfigurationUtils() {

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
    public static int getInt(Map<String, String> settings, String name) throws IllegalArgumentException {
        String value = getString(settings, name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Setting '" + name + "=" + value + "' is not an integer on " + settings);
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
    public static int getInt(Map<String, String> settings, String name, int defaultValue) throws IllegalArgumentException {
        if (settings.containsKey(name)) {

            String value = settings.get(name).toString();
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Setting '" + name + "=" + value + "' is not an integer on " + settings);
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
    public static long getLong(Map<String, String> settings, String name, long defaultValue) throws IllegalArgumentException {
        if (settings.containsKey(name)) {

            String value = settings.get(name).toString();
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Setting '" + name + "=" + value + "' is not a long on " + settings);
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
    public static boolean getBoolean(Map<String, String> settings, String name, boolean defaultValue) {
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
    public static String getString(Map<String, String> settings, String name) throws IllegalArgumentException {
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
    public static String getString(Map<String, String> settings, String name, String defaultValue) {
        if (settings.containsKey(name)) {
            return settings.get(name).toString();
        } else {
            return defaultValue;
        }
    }
}
