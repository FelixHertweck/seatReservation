/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
package de.felixhertweck.seatreservation.security.service;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.security.exceptions.AltchaVerificationException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.altcha.altcha.v1.Altcha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AltchaServiceTest {

    private RedisDataSource redisDataSource;
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;
    private ObjectMapper objectMapper;
    private AltchaService altchaService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        redisDataSource = mock(RedisDataSource.class);
        valueCommands = mock(ValueCommands.class);
        keyCommands = mock(KeyCommands.class);
        objectMapper = new ObjectMapper();

        when(redisDataSource.value(String.class)).thenReturn(valueCommands);
        when(redisDataSource.key(String.class)).thenReturn(keyCommands);

        altchaService = new AltchaService(redisDataSource, objectMapper);

        setField(altchaService, "enabled", true);
        setField(altchaService, "hmacKey", "test-secret-key-1234567890");
        setField(altchaService, "maxNumber", 10_000L);
        setField(altchaService, "expiresSeconds", 300L);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testCreateChallenge() {
        Altcha.Challenge challenge = altchaService.createChallenge();
        assertNotNull(challenge);
        assertNotNull(challenge.challenge());
        assertNotNull(challenge.salt());
        assertNotNull(challenge.signature());
        assertEquals("SHA-256", challenge.algorithm());
    }

    @Test
    void testVerifyAndConsume_NullOrBlankPayload() {
        assertThrows(AltchaVerificationException.class, () -> altchaService.verifyAndConsume(null));
        assertThrows(
                AltchaVerificationException.class, () -> altchaService.verifyAndConsume("   "));
    }

    @Test
    void testVerifyAndConsume_InvalidPayload() {
        assertThrows(
                AltchaVerificationException.class,
                () -> altchaService.verifyAndConsume("not-a-valid-base64-payload"));
    }

    @Test
    void testVerifyAndConsume_Success() throws Exception {
        Altcha.Challenge challenge = altchaService.createChallenge();
        Altcha.Algorithm algorithm = Altcha.Algorithm.fromString(challenge.algorithm());
        Altcha.Solution solution =
                Altcha.solveChallenge(
                        challenge.challenge(), challenge.salt(), algorithm, 10_000L, 0L);
        assertNotNull(solution);

        // Build valid base64 payload
        String jsonPayload =
                objectMapper.writeValueAsString(
                        new Altcha.Payload(
                                challenge.algorithm(),
                                challenge.challenge(),
                                solution.number(),
                                challenge.salt(),
                                challenge.signature()));
        String base64Payload =
                Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));

        when(valueCommands.setnx(startsWith("altcha:used:"), eq("1"))).thenReturn(true);

        assertDoesNotThrow(() -> altchaService.verifyAndConsume(base64Payload));
        verify(valueCommands).setnx(startsWith("altcha:used:"), eq("1"));
        verify(keyCommands).expire(startsWith("altcha:used:"), any());
    }

    @Test
    void testVerifyAndConsume_ReplayRejected() throws Exception {
        Altcha.Challenge challenge = altchaService.createChallenge();
        Altcha.Algorithm algorithm = Altcha.Algorithm.fromString(challenge.algorithm());
        Altcha.Solution solution =
                Altcha.solveChallenge(
                        challenge.challenge(), challenge.salt(), algorithm, 10_000L, 0L);
        assertNotNull(solution);

        String jsonPayload =
                objectMapper.writeValueAsString(
                        new Altcha.Payload(
                                challenge.algorithm(),
                                challenge.challenge(),
                                solution.number(),
                                challenge.salt(),
                                challenge.signature()));
        String base64Payload =
                Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));

        // Simulate key already exists in Redis (setnx returns false)
        when(valueCommands.setnx(startsWith("altcha:used:"), eq("1"))).thenReturn(false);

        AltchaVerificationException ex =
                assertThrows(
                        AltchaVerificationException.class,
                        () -> altchaService.verifyAndConsume(base64Payload));
        assertTrue(ex.getMessage().contains("already been used"));
    }
}
