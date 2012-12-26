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
package org.jmxexporter.util.json;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class PlaceholderEnabledJsonNodeFactory extends JsonNodeFactory {

    private static final long serialVersionUID = 1L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private PropertyPlaceholderResolver resolver = new PropertyPlaceholderResolver();

    public PlaceholderEnabledJsonNodeFactory(boolean bigDecimalExact) {
        super(bigDecimalExact);
    }

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
