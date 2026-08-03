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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Transparently encrypts/decrypts sensitive string columns (e.g. the TOTP secret) at rest using
 * AES-256-GCM, so a database dump alone does not expose the plaintext secret. The key is read from
 * a file (a base64-encoded 32-byte key, e.g. generated with {@code openssl rand -base64 32}) at the
 * path given by the {@code security.encryption-key-location} config property -- kept out of the
 * database and, unlike an env var, not visible via {@code docker inspect}. Mirrors how the JWT
 * signing keys are handled (see {@code keys/privateKey.pem}).
 *
 * <p>Applied explicitly via {@code @Convert}, not auto-applied, so only fields that opt in are
 * affected.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // The key file never changes at runtime, so it's read once and cached rather than re-read on
    // every encrypt/decrypt call.
    private static volatile SecretKeySpec cachedKey;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = SecurityUtils.generateRandomBytes(GCM_IV_LENGTH_BYTES);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt column value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Encrypted column value is not valid Base64", e);
        }
        if (combined.length < GCM_IV_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "Encrypted column value is too short to contain a GCM IV -- corrupted data?");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt column value", e);
        }
    }

    private SecretKeySpec secretKey() {
        SecretKeySpec key = cachedKey;
        if (key == null) {
            synchronized (EncryptedStringConverter.class) {
                key = cachedKey;
                if (key == null) {
                    key = loadKey();
                    cachedKey = key;
                }
            }
        }
        return key;
    }

    private static SecretKeySpec loadKey() {
        String location =
                ConfigProvider.getConfig()
                        .getValue("security.encryption-key-location", String.class);
        String base64Key;
        try {
            base64Key = Files.readString(Path.of(location)).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read encryption key from " + location, e);
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Encryption key at " + location + " is not valid Base64", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Encryption key at "
                            + location
                            + " must decode to exactly 32 bytes for AES-256 (got "
                            + keyBytes.length
                            + "); generate one with `openssl rand -base64 32`");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
