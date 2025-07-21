package de.felixhertweck.seatreservation.sanitization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

public class XssSanitizingDeserializer extends JsonDeserializer<String>
        implements ContextualDeserializer {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = StringDeserializer.instance.deserialize(p, ctxt);
        if (value == null) {
            return null;
        }
        return HtmlSanitizerUtils.sanitize(value);
    }

    @Override
    public JsonDeserializer<?> createContextual(
            DeserializationContext ctxt, BeanProperty property) {
        if (property != null && property.getAnnotation(NoHtmlSanitize.class) != null) {
            return StringDeserializer.instance;
        }
        return this;
    }
}
