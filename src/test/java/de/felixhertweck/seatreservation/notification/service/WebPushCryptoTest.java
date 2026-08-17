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
package de.felixhertweck.seatreservation.notification.service;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link WebPushCrypto} against independent re-implementations of the subscriber-side
 * decryption and JWT verification, rather than only checking it against itself.
 */
class WebPushCryptoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void encrypt_roundTripsWithIndependentDecryption() throws Exception {
        KeyPair user = WebPushCrypto.generateKeyPair();
        byte[] userPublicRaw =
                WebPushCrypto.encodeUncompressedPoint((ECPublicKey) user.getPublic());
        byte[] authSecret = randomBytes(16);
        byte[] plaintext = "{\"title\":\"Buchungsbestätigung\"}".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = WebPushCrypto.encrypt(plaintext, userPublicRaw, authSecret);
        byte[] decrypted =
                decryptForTest(
                        ciphertext, (ECPrivateKey) user.getPrivate(), userPublicRaw, authSecret);

        assertArrayEquals(plaintext, decrypted);
    }

    @RepeatedTest(50)
    void encrypt_roundTripsAcrossManyRandomKeys() throws Exception {
        // Repeated with fresh random keys each time to catch the classic ECDH/BigInteger
        // leading-zero-byte truncation bug, which only shows up intermittently.
        KeyPair user = WebPushCrypto.generateKeyPair();
        byte[] userPublicRaw =
                WebPushCrypto.encodeUncompressedPoint((ECPublicKey) user.getPublic());
        byte[] authSecret = randomBytes(16);
        byte[] plaintext = ("payload-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = WebPushCrypto.encrypt(plaintext, userPublicRaw, authSecret);
        byte[] decrypted =
                decryptForTest(
                        ciphertext, (ECPrivateKey) user.getPrivate(), userPublicRaw, authSecret);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_rejectsMalformedSubscriberKey() {
        byte[] tooShort = new byte[] {1, 2, 3};
        assertThrows(
                GeneralSecurityException.class,
                () ->
                        WebPushCrypto.encrypt(
                                "x".getBytes(StandardCharsets.UTF_8), tooShort, randomBytes(16)));
    }

    @Test
    void buildVapidAuthorizationHeader_producesAValidSignedJwt() throws Exception {
        KeyPair vapid = WebPushCrypto.generateKeyPair();
        String publicKeyBase64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                WebPushCrypto.encodeUncompressedPoint(
                                        (ECPublicKey) vapid.getPublic()));

        String header =
                WebPushCrypto.buildVapidAuthorizationHeader(
                        objectMapper,
                        "https://fcm.googleapis.com/wp/abc123",
                        vapid.getPrivate(),
                        publicKeyBase64,
                        "mailto:test@example.com");

        assertTrue(header.startsWith("vapid t="));
        assertTrue(header.endsWith(", k=" + publicKeyBase64));

        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length, "a JWT has header.claims.signature");

        String claimsJson =
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertTrue(claimsJson.contains("\"aud\":\"https://fcm.googleapis.com\""));
        assertTrue(claimsJson.contains("\"sub\":\"mailto:test@example.com\""));

        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(vapid.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(
                verifier.verify(signature), "signature must verify against the VAPID public key");
    }

    @Test
    void publicKey_encodeDecode_roundTrips() throws Exception {
        KeyPair keyPair = WebPushCrypto.generateKeyPair();
        byte[] raw = WebPushCrypto.encodeUncompressedPoint((ECPublicKey) keyPair.getPublic());

        assertEquals(65, raw.length);
        assertEquals(0x04, raw[0]);
        ECPublicKey decoded = WebPushCrypto.decodePublicKey(raw);
        assertEquals(((ECPublicKey) keyPair.getPublic()).getW(), decoded.getW());
    }

    @Test
    void privateKey_decode_producesAUsableSigningKey() throws Exception {
        KeyPair keyPair = WebPushCrypto.generateKeyPair();
        byte[] rawScalar = toFixedLength(((ECPrivateKey) keyPair.getPrivate()).getS());
        ECPrivateKey decoded = WebPushCrypto.decodePrivateKey(rawScalar);

        Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(decoded);
        signer.update("test".getBytes(StandardCharsets.US_ASCII));
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keyPair.getPublic());
        verifier.update("test".getBytes(StandardCharsets.US_ASCII));
        assertTrue(verifier.verify(signature));
    }

    // ---- test-only helpers ----

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static byte[] toFixedLength(BigInteger value) {
        byte[] src = value.toByteArray();
        byte[] dst = new byte[32];
        if (src.length == 32) {
            System.arraycopy(src, 0, dst, 0, 32);
        } else if (src.length > 32) {
            System.arraycopy(src, src.length - 32, dst, 0, 32);
        } else {
            System.arraycopy(src, 0, dst, 32 - src.length, src.length);
        }
        return dst;
    }

    /**
     * Independent re-implementation of the subscriber-side aes128gcm decryption (RFC 8291/8188), so
     * the round-trip tests don't just check {@link WebPushCrypto#encrypt} against itself.
     */
    private static byte[] decryptForTest(
            byte[] encryptedRecord,
            ECPrivateKey userPrivateKey,
            byte[] userPublicKeyRaw,
            byte[] authSecret)
            throws GeneralSecurityException {
        ByteBuffer buf = ByteBuffer.wrap(encryptedRecord);
        byte[] salt = new byte[16];
        buf.get(salt);
        buf.getInt(); // record size, unused for a single-record message
        int idLength = buf.get() & 0xff;
        byte[] asPublicRaw = new byte[idLength];
        buf.get(asPublicRaw);
        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        ECPublicKey asPublicKey = WebPushCrypto.decodePublicKey(asPublicRaw);

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(userPrivateKey);
        keyAgreement.doPhase(asPublicKey, true);
        byte[] sharedSecret = toFixedLength(new BigInteger(1, keyAgreement.generateSecret()));

        byte[] prkKey = hmacSha256(authSecret, sharedSecret);
        byte[] keyInfo =
                concat(
                        "WebPush: info\0".getBytes(StandardCharsets.US_ASCII),
                        userPublicKeyRaw,
                        asPublicRaw);
        byte[] ikm = hkdfExpand(prkKey, keyInfo, 32);

        byte[] prk = hmacSha256(salt, ikm);
        byte[] cek =
                hkdfExpand(
                        prk,
                        "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII),
                        16);
        byte[] nonce =
                hkdfExpand(
                        prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(cek, "AES"),
                new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);

        return Arrays.copyOf(padded, padded.length - 1); // strip the 0x02 last-record delimiter
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] hkdfExpand(byte[] prk, byte[] info, int length)
            throws GeneralSecurityException {
        byte[] block = hmacSha256(prk, concat(info, new byte[] {0x01}));
        return Arrays.copyOf(block, length);
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] part : parts) {
            total += part.length;
        }
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }
}
