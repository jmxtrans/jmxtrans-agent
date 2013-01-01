/*
 * Copyright 1012-2013 the original author or authors.
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
package org.jmxexporter.util.json;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property placeholder / substitution enabled {@link JsonNodeFactory}.
 * <p/>
 * Each JSON text value is processed by the {@link PropertyPlaceholderResolver} before building a {@linkplain TextNode}.
 * <p/>
 * Inspired by <a href="http://static.springsource.org/spring/docs/3.1.x/spring-framework-reference/html/beans.html#beans-factory-placeholderconfigurer">
 * Spring Framework PropertyPlaceHolder</a> and by
 * <a href="http://commons.apache.org/digester/guide/substitution.html">Jakarta Digester Substitution</a>.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class PlaceholderEnabledJsonNodeFactory extends JsonNodeFactory {

    private static final long serialVersionUID = 1L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private PropertyPlaceholderResolver resolver = new PropertyPlaceholderResolver();

    /**
     * Main constructor
     * <p/>
     * <p>The only argument to this constructor is a boolean telling whether
     * {@link com.fasterxml.jackson.databind.node.DecimalNode} instances must be built with exact representations of
     * {@link java.math.BigDecimal} instances.</p>
     * <p/>
     * <p>This has quite an influence since, for instance, a BigDecimal (and,
     * therefore, a DecimalNode) constructed from input string {@code "1.0"} and
     * another constructed with input string {@code "1.00"} <b>will not</b> be
     * equal, since their scale differs (1 in the first case, 2 in the second
     * case).</p>
     * <p/>
     * <p>Note that setting the argument to {@code true} does <i>not</i>
     * guarantee a strict inequality between JSON representations: input texts
     * {@code "0.1"} and {@code "1e-1"}, for instance, yield two equivalent
     * BigDecimal instances since they have the same scale (1).</p>
     * <p/>
     * <p>The no-arg constructor (and the default {@link #instance}) calls this
     * constructor with {@code false} as an argument.</p>
     *
     * @param bigDecimalExact see description
     * @see java.math.BigDecimal
     */
    public PlaceholderEnabledJsonNodeFactory(boolean bigDecimalExact) {
        super(bigDecimalExact);
    }

    /**
     * Default constructor
     * <p/>
     * <p>This calls {@link #PlaceholderEnabledJsonNodeFactory(boolean)} with {@code false}
     * as an argument.</p>
     */
    public PlaceholderEnabledJsonNodeFactory() {
        super();
    }

    @Override
    public TextNode textNode(String text) {
        String resolvedText = resolver.resolveString(text);
        if (logger.isInfoEnabled()) {
            if (text == null ? resolvedText == null : text.equals(resolvedText)) {
                logger.debug("Resolve '{}' into '{}'", text, resolvedText);
            } else {
                logger.info("Resolve '{}' into '{}'", text, resolvedText);
            }
        }
        return super.textNode(resolvedText);
    }
}
