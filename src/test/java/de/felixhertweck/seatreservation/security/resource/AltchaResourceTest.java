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
package de.felixhertweck.seatreservation.security.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.security.service.AltchaService;
import org.altcha.altcha.v1.Altcha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AltchaResourceTest {

    @Mock private AltchaService altchaService;

    @InjectMocks private AltchaResource altchaResource;

    private Altcha.Challenge fakeChallenge;

    @BeforeEach
    void setUp() {
        fakeChallenge =
                new Altcha.Challenge(
                        "SHA-256", "abc123challenge", 100000L, "abc123salt", "abc123sig");
    }

    @Test
    void testGetChallenge_DelegatesToService() {
        when(altchaService.createChallenge()).thenReturn(fakeChallenge);

        Altcha.Challenge result = altchaResource.getChallenge();

        assertNotNull(result);
        assertEquals("SHA-256", result.algorithm());
        assertEquals("abc123challenge", result.challenge());
        assertEquals("abc123salt", result.salt());
        assertEquals("abc123sig", result.signature());
    }
}
