package org.jmxtrans.agent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public interface ExpressionLanguageEngine {
    /**
     * Replace all the '#' based keywords (e.g. <code>#hostname#</code>) by the value returned by the associated function.
     *
     * @param expression the expression to resolve (e.g. <code>"servers.#hostname#."</code>)
     * @return the resolved expression (e.g. <code>"servers.tomcat5"</code>)
     */
    @Nonnull
    String resolveExpression(@Nonnull String expression);

    @Nonnull
    String resolveExpression(@Nonnull String expression, @Nonnull ObjectName exactObjectName, @Nullable String attribute, @Nullable String compositeDataKey, @Nullable Integer position);
}
