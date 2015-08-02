package org.jmxtrans.agent;

import org.jmxtrans.agent.util.StringUtils2;
import org.jmxtrans.agent.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.ObjectName;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds names with general rules like JConsole / VisualVM do.
 * i.e. {@code <domain-name>.<property-name><property-name><attribute-name><composite-data-key-name>}
 *
 * E.g. For objectName = "type:name=metric,value=bar" and attribute "count",
 * it will general resultName = "type.metric.bar.count"
 *
 * @author <a href="mailto:maheshkelkar@gmail.com">Mahesh V Kelkar</a>
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JConsoleResultNameStrategyImpl implements ResultNameStrategy {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private ExpressionLanguageEngine expressionLanguageEngine = new ExpressionLanguageEngineImpl();

    @Nonnull
    @Override
    public String getResultName(@Nonnull Query query, @Nonnull ObjectName objectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position) {
        String result;
        if (query.getResultAlias() == null) {
            result = escapeObjectName(objectName);
            if (!StringUtils2.isNullOrEmpty(attribute)) {
                result += "." + attribute;
            }
            if (!StringUtils2.isNullOrEmpty(compositeDataKey)) {
                result += "." + compositeDataKey;
            }
            if (position != null) {
                result += "_" + position;
            }
        } else {
            result = expressionLanguageEngine.resolveExpression(query.getResultAlias(), objectName, attribute, compositeDataKey, position);
        }
        return result;
    }

    /**
     * Transforms an {@linkplain javax.management.ObjectName} into a plain {@linkplain String}
     * only composed of ('a' to 'Z', 'A' to 'Z', '.', '_') similar to JConsole naming.
     *
     * '_' is the escape char for not compliant chars.
     */
    protected String escapeObjectName(@Nonnull ObjectName objectName) {

        /** Add objectName's domain */
        StringBuilder result = new StringBuilder();
        StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getDomain(), false, result);

        /** Walk through (sorted) properties of the ObjectName and add values to the result */
        List<String> keys = Collections.list(objectName.getKeyPropertyList().keys());
        Collections.sort(keys);
        for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String propertyKey = it.next();
            result.append('.');
            StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getKeyProperty(propertyKey), false, result);
        }

        return result.toString();
    }

    public ExpressionLanguageEngine getExpressionLanguageEngine() {
        return expressionLanguageEngine;
    }

    public void setExpressionLanguageEngine(ExpressionLanguageEngine expressionLanguageEngine) {
        this.expressionLanguageEngine = expressionLanguageEngine;
    }

    public void postConstruct(@Nonnull Map<String, String> settings) {

    }
}
