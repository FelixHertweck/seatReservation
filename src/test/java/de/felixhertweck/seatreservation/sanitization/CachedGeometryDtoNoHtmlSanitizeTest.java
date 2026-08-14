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

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.AreaResponseDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceResponseDTO;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Guards against a regression where deserializing these projections on a Redis cache hit (see
 * {@code SeatmapCacheService}) re-ran every string field through the XSS sanitizer, even though the
 * values were already sanitized once at write time via the corresponding {@code *RequestDTO}. Uses
 * the same CDI-managed, customized {@link ObjectMapper} that the Redis cache codec resolves for its
 * own (de)serialization, so this exercises the exact mechanism at play, not just the annotation's
 * presence.
 */
@QuarkusTest
class CachedGeometryDtoNoHtmlSanitizeTest {

    private static final String RAW = "<script>alert('XSS')</script>Hello";

    @Inject ObjectMapper objectMapper;

    @Test
    void seatDTO_StringFields_SurviveDeserializationUnsanitized() throws Exception {
        String json =
                "{\"id\":\"11111111-1111-1111-1111-111111111111\","
                        + "\"seatNumber\":\""
                        + RAW
                        + "\",\"seatRow\":\""
                        + RAW
                        + "\","
                        + "\"locationId\":\"22222222-2222-2222-2222-222222222222\","
                        + "\"coordinate\":{\"xCoordinate\":1,\"yCoordinate\":2},"
                        + "\"entrance\":\""
                        + RAW
                        + "\",\"area\":\""
                        + RAW
                        + "\"}";

        SeatDTO dto = objectMapper.readValue(json, SeatDTO.class);

        assertEquals(RAW, dto.seatNumber());
        assertEquals(RAW, dto.seatRow());
        assertEquals(RAW, dto.entrance());
        assertEquals(RAW, dto.area());
    }

    @Test
    void areaResponseDTO_Name_SurvivesDeserializationUnsanitized() throws Exception {
        String json =
                "{\"id\":\"11111111-1111-1111-1111-111111111111\","
                        + "\"name\":\""
                        + RAW
                        + "\",\"boundary\":[],"
                        + "\"eventLocationId\":\"22222222-2222-2222-2222-222222222222\"}";

        AreaResponseDTO dto = objectMapper.readValue(json, AreaResponseDTO.class);

        assertEquals(RAW, dto.name());
    }

    @Test
    void entranceResponseDTO_Name_SurvivesDeserializationUnsanitized() throws Exception {
        String json =
                "{\"id\":\"11111111-1111-1111-1111-111111111111\","
                        + "\"name\":\""
                        + RAW
                        + "\","
                        + "\"eventLocationId\":\"22222222-2222-2222-2222-222222222222\"}";

        EntranceResponseDTO dto = objectMapper.readValue(json, EntranceResponseDTO.class);

        assertEquals(RAW, dto.name());
    }

    @Test
    void eventLocationMakerDTO_Label_SurvivesDeserializationUnsanitized() throws Exception {
        String json =
                "{\"id\":\"11111111-1111-1111-1111-111111111111\","
                        + "\"label\":\""
                        + RAW
                        + "\",\"coordinate\":{\"xCoordinate\":1,\"yCoordinate\":2},"
                        + "\"eventLocationId\":\"22222222-2222-2222-2222-222222222222\"}";

        EventLocationMakerDTO dto = objectMapper.readValue(json, EventLocationMakerDTO.class);

        assertEquals(RAW, dto.label());
    }
}
