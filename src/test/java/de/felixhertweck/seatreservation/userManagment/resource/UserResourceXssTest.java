package de.felixhertweck.seatreservation.userManagment.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.felixhertweck.seatreservation.sanitization.XssSanitizingDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserResourceXssTest {

    private ObjectMapper objectMapper;

    // A simple test class to deserialize into
    private static class TestDto {
        public String text;
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new XssSanitizingDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    void whenDeserializing_thenStripsHtmlTags() throws Exception {
        String json = "{\"text\":\"Hello <b>world</b>!\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);
        assertEquals("Hello world!", dto.text);
    }

    @Test
    void whenDeserializing_thenStripsScriptTags() throws Exception {
        String json = "{\"text\":\"<script>alert('xss')</script>Some text\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);
        assertEquals("Some text", dto.text);
    }

    @Test
    void whenDeserializingNull_thenReturnsNull() throws Exception {
        String json = "{\"text\":null}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);
        assertNull(dto.text);
    }

    @Test
    void whenDeserializingEmptyString_thenReturnsEmptyString() throws Exception {
        String json = "{\"text\":\"\"}";
        TestDto dto = objectMapper.readValue(json, TestDto.class);
        assertEquals("", dto.text);
    }
}
