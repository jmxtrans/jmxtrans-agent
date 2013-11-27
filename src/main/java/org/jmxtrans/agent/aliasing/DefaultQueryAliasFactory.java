package org.jmxtrans.agent.aliasing;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.management.ObjectName;

import org.jmxtrans.agent.Query;
import org.jmxtrans.agent.util.StringUtils2;

public enum DefaultQueryAliasFactory implements QueryAliasFactory {
	/* */
	INSTANCE;
	
	private final Logger logger = Logger.getLogger(getClass().getName());	
	
	public String valueOf(Query query, ObjectName objectName, String key) {
		if (key == null) {
			return escapeObjectName(objectName) + "." + query.getAttribute();
        }
        return escapeObjectName(objectName) + "." + query.getAttribute() + "." + key;
	}

	/**
	 * Transforms an {@linkplain javax.management.ObjectName} into a plain {@linkplain String} only composed of (a->Z, A-Z, '_').
	 * <p/>
	 * '_' is the escape char for not compliant chars.
	 */
    private String escapeObjectName(@Nonnull ObjectName objectName) {
        StringBuilder result = new StringBuilder();
        StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getDomain(), result);
        result.append('.');
        List<String> keys = Collections.list(objectName.getKeyPropertyList().keys());
        Collections.sort(keys);
        for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String key = it.next();
            StringUtils2.appendEscapedNonAlphaNumericChars(key, result);
            result.append("__");
            StringUtils2.appendEscapedNonAlphaNumericChars(objectName.getKeyProperty(key), result);
            if (it.hasNext()) {
                result.append('.');
            }
        }
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "escapeObjectName(" + objectName + "): " + result);
        return result.toString();
    }
}
