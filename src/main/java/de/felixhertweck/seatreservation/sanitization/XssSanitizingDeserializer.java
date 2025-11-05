/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
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
 * #L%
 */
package de.felixhertweck.seatreservation.sanitization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.jboss.logging.Logger;

public class XssSanitizingDeserializer extends JsonDeserializer<String>
        implements ContextualDeserializer {

    private static final Logger LOG = Logger.getLogger(XssSanitizingDeserializer.class);

    /**
     * Deserializes a string value from JSON and applies XSS sanitization. The sanitization can be
     * skipped if the field is marked with @NoHtmlSanitize.
     *
     * @param p the JSON parser
     * @param ctxt the deserialization context
     * @return the sanitized string value
     * @throws IOException if an I/O error occurs during parsing
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = StringDeserializer.instance.deserialize(p, ctxt);
        if (value == null) {
            LOG.debug("Deserialized value is null, returning null.");
            return null;
        }
        String sanitizedValue = HtmlSanitizerUtils.sanitize(value);
        if (!value.equals(sanitizedValue)) {
            LOG.debugf(
                    "Sanitized XSS input. Original: '%s', Sanitized: '%s'", value, sanitizedValue);
        } else {
            LOG.debugf("XSS input did not require sanitization: '%s'", value);
        }
        return sanitizedValue;
    }

    /**
     * Creates a contextual deserializer instance for a specific bean property. If the property is
     * annotated with @NoHtmlSanitize, returns the default StringDeserializer to skip sanitization.
     * Otherwise, returns this sanitizing deserializer instance.
     *
     * @param ctxt the deserialization context
     * @param property the bean property being deserialized
     * @return a JsonDeserializer for String values, either sanitizing or standard
     */
    @Override
    public JsonDeserializer<?> createContextual(
            DeserializationContext ctxt, BeanProperty property) {
        if (property != null && property.getAnnotation(NoHtmlSanitize.class) != null) {
            LOG.debugf(
                    "Property '%s' is annotated with @NoHtmlSanitize, skipping XSS sanitization.",
                    property.getName());
            return StringDeserializer.instance;
        }
        return this;
    }
}
