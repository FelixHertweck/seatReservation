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
package de.felixhertweck.seatreservation.utils;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Bypasses {@code loadKey()}'s config-file lookup by injecting a random test key directly into the
 * static cache, since the converter's actual crypto logic doesn't depend on where the key came
 * from.
 */
class EncryptedStringConverterTest {

    private static final String KEY_LOCATION_PROPERTY = "security.encryption-key-location";

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @TempDir Path tempDir;

    @BeforeEach
    void injectTestKey() throws Exception {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        setCachedKey(new SecretKeySpec(keyBytes, "AES"));
    }

    @AfterEach
    void resetCachedKey() throws Exception {
        setCachedKey(null);
        System.clearProperty(KEY_LOCATION_PROPERTY);
    }

    private static void setCachedKey(SecretKeySpec key) throws Exception {
        Field field = EncryptedStringConverter.class.getDeclaredField("cachedKey");
        field.setAccessible(true);
        field.set(null, key);
    }

    @Test
    void convertToDatabaseColumn_Null_ReturnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_Null_ReturnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void roundTrip_EncryptsAndDecryptsBackToOriginal() {
        String plaintext = "JBSWY3DPEHPK3PXP";

        String encrypted = converter.convertToDatabaseColumn(plaintext);

        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, converter.convertToEntityAttribute(encrypted));
    }

    @Test
    void roundTrip_UsesRandomIv_SameInputEncryptsDifferently() {
        String plaintext = "JBSWY3DPEHPK3PXP";

        String first = converter.convertToDatabaseColumn(plaintext);
        String second = converter.convertToDatabaseColumn(plaintext);

        assertNotEquals(first, second);
        assertEquals(plaintext, converter.convertToEntityAttribute(first));
        assertEquals(plaintext, converter.convertToEntityAttribute(second));
    }

    @Test
    void convertToEntityAttribute_InvalidBase64_ThrowsIllegalStateException() {
        assertThrows(
                IllegalStateException.class,
                () -> converter.convertToEntityAttribute("not-valid-base64!!!"));
    }

    @Test
    void convertToEntityAttribute_TooShortForGcmIv_ThrowsIllegalStateException() {
        // 3 bytes is shorter than the 12-byte GCM IV the converter expects to find at the start.
        String tooShort = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> converter.convertToEntityAttribute(tooShort));
        assertTrue(ex.getMessage().contains("too short"));
    }

    @Test
    void convertToEntityAttribute_TamperedCiphertext_ThrowsIllegalStateException() {
        String encrypted = converter.convertToDatabaseColumn("JBSWY3DPEHPK3PXP");
        byte[] combined = Base64.getDecoder().decode(encrypted);
        // Flip a byte inside the ciphertext (past the 12-byte IV) so the GCM auth tag no longer
        // matches -- decryption must fail loudly rather than return corrupted plaintext.
        combined[combined.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(combined);

        assertThrows(
                IllegalStateException.class, () -> converter.convertToEntityAttribute(tampered));
    }

    @Test
    void loadKey_InvalidBase64_ThrowsIllegalStateException() throws Exception {
        Path keyFile = tempDir.resolve("bad-key");
        Files.writeString(keyFile, "not-valid-base64!!!");
        System.setProperty(KEY_LOCATION_PROPERTY, keyFile.toString());
        setCachedKey(null); // force loadKey() to run instead of using the injected test key

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> converter.convertToDatabaseColumn("secret"));
        assertTrue(ex.getMessage().contains("Base64"));
    }

    @Test
    void loadKey_WrongLength_ThrowsIllegalStateException() throws Exception {
        Path keyFile = tempDir.resolve("short-key");
        // 16 bytes (AES-128) instead of the required 32 (AES-256).
        Files.writeString(keyFile, Base64.getEncoder().encodeToString(new byte[16]));
        System.setProperty(KEY_LOCATION_PROPERTY, keyFile.toString());
        setCachedKey(null);

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> converter.convertToDatabaseColumn("secret"));
        assertTrue(ex.getMessage().contains("32 bytes"));
    }
}
