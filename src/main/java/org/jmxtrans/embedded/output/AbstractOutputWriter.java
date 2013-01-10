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
import org.jmxtrans.embedded.ResultNameStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience abstract class to implement an {@link OutputWriter}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public abstract class AbstractOutputWriter implements OutputWriter {

    public final static String SETTING_PORT = "port";
    public final static String SETTING_HOST = "host";
    public final static String SETTING_NAME_PREFIX = "namePrefix";

    private ResultNameStrategy strategy = new ResultNameStrategy();

    private Map<String, Object> settings = new HashMap<String, Object>();

    private boolean enabled = true;

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
                "enabled=" + enabled +
                ", settings=" + settings +
                '}';
    }


    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public ResultNameStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ResultNameStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractOutputWriter that = (AbstractOutputWriter) o;

        if (!settings.equals(that.settings)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


}
