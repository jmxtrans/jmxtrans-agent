package org.jmxtrans.agent;

import javax.annotation.Nonnull;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public interface ExpressionLanguageEngine {
    /**
     * Replace all the '#' based keywords (e.g. <code>#hostname#</code>) by their value.
     *
     * @param expression the expression to resolve (e.g. <code>"servers.#hostname#."</code>)
     * @return the resolved expression (e.g. <code>"servers.tomcat5"</code>)
     */
    @Nonnull
    String resolveExpression(@Nonnull String expression);

    String resolveExpression(@Nonnull String expression, @Nonnull ObjectName exactObjectName);
}
