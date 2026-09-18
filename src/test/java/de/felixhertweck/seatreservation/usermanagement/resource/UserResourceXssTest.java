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
package de.felixhertweck.seatreservation.usermanagement.resource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.felixhertweck.seatreservation.sanitization.XssSanitizingDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    private static Stream<Arguments> deserializationCases() {
        return Stream.of(
                Arguments.of("{\"text\":\"Hello <b>world</b>!\"}", "Hello world!"),
                Arguments.of("{\"text\":\"<script>alert('xss')</script>Some text\"}", "Some text"),
                Arguments.of("{\"text\":null}", null),
                Arguments.of("{\"text\":\"\"}", ""));
    }

    @ParameterizedTest
    @MethodSource("deserializationCases")
    void whenDeserializing_thenSanitizesAsExpected(String json, String expected) throws Exception {
        TestDto dto = objectMapper.readValue(json, TestDto.class);
        assertEquals(expected, dto.text);
    }
}
