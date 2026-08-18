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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.utils.SecurityUtils;

/**
 * Implements the two pieces of cryptography needed to deliver a Web Push message: payload
 * encryption per RFC 8291 (using the "aes128gcm" content-coding from RFC 8188) and VAPID request
 * authentication per RFC 8292.
 */
final class WebPushCrypto {

    private static final String EC_ALGORITHM = "EC";
    private static final String EC_CURVE = "secp256r1";

    /** Uncompressed EC point: 0x04 || X(32) || Y(32). */
    private static final int UNCOMPRESSED_POINT_LENGTH = 65;

    private static final int COORDINATE_LENGTH = 32;
    private static final int AUTH_TAG_LENGTH_BITS = 128;
    private static final int RECORD_SIZE = 4096;

    private static final byte[] WEBPUSH_INFO_PREFIX =
            "WebPush: info\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CEK_INFO =
            "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NONCE_INFO =
            "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII);

    private static final ECParameterSpec EC_PARAMETER_SPEC = loadEcParameterSpec();

    private WebPushCrypto() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static ECParameterSpec loadEcParameterSpec() {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance(EC_ALGORITHM);
            params.init(new ECGenParameterSpec(EC_CURVE));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(EC_CURVE + " curve parameters unavailable", e);
        }
    }

    // ---- Key encoding ----

    static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_ALGORITHM);
        generator.initialize(new ECGenParameterSpec(EC_CURVE), SecurityUtils.getSecureRandom());
        return generator.generateKeyPair();
    }

    /** Encodes an EC public key as the 65-byte uncompressed point format the Push API uses. */
    static byte[] encodeUncompressedPoint(ECPublicKey key) {
        byte[] result = new byte[UNCOMPRESSED_POINT_LENGTH];
        result[0] = 0x04;
        writeFixedLength(key.getW().getAffineX(), result, 1);
        writeFixedLength(key.getW().getAffineY(), result, 1 + COORDINATE_LENGTH);
        return result;
    }

    /** Decodes a 65-byte uncompressed EC point, as sent by browsers for {@code p256dh}. */
    static ECPublicKey decodePublicKey(byte[] uncompressedPoint) throws GeneralSecurityException {
        if (uncompressedPoint == null
                || uncompressedPoint.length != UNCOMPRESSED_POINT_LENGTH
                || uncompressedPoint[0] != 0x04) {
            throw new InvalidKeyException("Expected a 65-byte uncompressed EC point");
        }
        BigInteger x =
                new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, 1, 1 + COORDINATE_LENGTH));
        BigInteger y =
                new BigInteger(
                        1,
                        Arrays.copyOfRange(
                                uncompressedPoint,
                                1 + COORDINATE_LENGTH,
                                UNCOMPRESSED_POINT_LENGTH));
        ECPublicKeySpec spec = new ECPublicKeySpec(new ECPoint(x, y), EC_PARAMETER_SPEC);
        return (ECPublicKey) KeyFactory.getInstance(EC_ALGORITHM).generatePublic(spec);
    }

    /** Decodes a raw 32-byte private scalar, our on-the-wire format for a configured VAPID key. */
    static ECPrivateKey decodePrivateKey(byte[] rawScalar) throws GeneralSecurityException {
        BigInteger s = new BigInteger(1, rawScalar);
        ECPrivateKeySpec spec = new ECPrivateKeySpec(s, EC_PARAMETER_SPEC);
        return (ECPrivateKey) KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(spec);
    }

    /** Writes a coordinate/scalar into {@code dest} at {@code offset}, left-padded to 32 bytes. */
    private static void writeFixedLength(BigInteger value, byte[] dest, int offset) {
        byte[] src = value.toByteArray();
        if (src.length == COORDINATE_LENGTH) {
            System.arraycopy(src, 0, dest, offset, COORDINATE_LENGTH);
        } else if (src.length > COORDINATE_LENGTH) {
            // A leading 0x00 sign byte from BigInteger's two's-complement encoding; drop it.
            System.arraycopy(src, src.length - COORDINATE_LENGTH, dest, offset, COORDINATE_LENGTH);
        } else {
            System.arraycopy(src, 0, dest, offset + (COORDINATE_LENGTH - src.length), src.length);
        }
    }

    private static byte[] toFixedLength(byte[] value) {
        byte[] dest = new byte[COORDINATE_LENGTH];
        writeFixedLength(new BigInteger(1, value), dest, 0);
        return dest;
    }

    // ---- Payload encryption (RFC 8291 "aes128gcm") ----

    /**
     * Encrypts {@code plaintext} for a subscription's {@code p256dh}/{@code auth} keys, returning a
     * ready-to-send aes128gcm body (RFC 8188 header followed by the single encrypted record).
     */
    static byte[] encrypt(byte[] plaintext, byte[] userPublicKeyRaw, byte[] authSecret)
            throws GeneralSecurityException {
        ECPublicKey userPublicKey = decodePublicKey(userPublicKeyRaw);

        KeyPair ephemeral = generateKeyPair();
        byte[] asPublicRaw = encodeUncompressedPoint((ECPublicKey) ephemeral.getPublic());

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(ephemeral.getPrivate());
        keyAgreement.doPhase(userPublicKey, true);
        // JDK's ECDH strips leading zero bytes from the shared secret; re-pad to a fixed 32 bytes.
        byte[] sharedSecret = toFixedLength(keyAgreement.generateSecret());

        byte[] prkKey = hmacSha256(authSecret, sharedSecret);
        byte[] keyInfo = concat(WEBPUSH_INFO_PREFIX, userPublicKeyRaw, asPublicRaw);
        byte[] ikm = hkdfExpand(prkKey, keyInfo, COORDINATE_LENGTH);

        byte[] salt = SecurityUtils.generateRandomBytes(16);
        byte[] prk = hmacSha256(salt, ikm);

        byte[] cek = hkdfExpand(prk, CEK_INFO, 16);
        byte[] nonce = hkdfExpand(prk, NONCE_INFO, 12);

        // A single record carries the whole message; 0x02 marks it as the final (and only) record.
        byte[] paddedPlaintext = concat(plaintext, new byte[] {0x02});

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(cek, "AES"),
                new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, nonce));
        byte[] ciphertext = cipher.doFinal(paddedPlaintext);

        ByteBuffer header =
                ByteBuffer.allocate(16 + 4 + 1 + UNCOMPRESSED_POINT_LENGTH); // network byte order
        header.put(salt);
        header.putInt(RECORD_SIZE);
        header.put((byte) UNCOMPRESSED_POINT_LENGTH);
        header.put(asPublicRaw);

        return concat(header.array(), ciphertext);
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    /**
     * HKDF-Expand (RFC 5869); a single HMAC block always suffices since every {@code length} we
     * need here is at most the 32-byte SHA-256 output.
     */
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

    // ---- VAPID (RFC 8292) ----

    /**
     * Builds the {@code Authorization: vapid ...} header value for a push request to {@code
     * endpoint}.
     */
    static String buildVapidAuthorizationHeader(
            ObjectMapper objectMapper,
            String endpoint,
            PrivateKey privateKey,
            String publicKeyBase64Url,
            String subject)
            throws GeneralSecurityException {
        try {
            String audience = originOf(endpoint);

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("typ", "JWT");
            header.put("alg", "ES256");

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("aud", audience);
            // RFC 8292 caps VAPID JWT lifetime at 24h; we use a comfortable margin below that.
            claims.put("exp", Instant.now().getEpochSecond() + 12 * 3600);
            claims.put("sub", subject);

            String signingInput =
                    base64Url(objectMapper.writeValueAsBytes(header))
                            + "."
                            + base64Url(objectMapper.writeValueAsBytes(claims));

            // The "P1363" format signature is the raw, fixed-length r||s pair JWS ES256 requires -
            // no ASN.1 DER decoding needed, unlike the JCA's default ECDSA signature format.
            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(privateKey, SecurityUtils.getSecureRandom());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));

            String jwt = signingInput + "." + base64Url(signature.sign());
            return "vapid t=" + jwt + ", k=" + publicKeyBase64Url;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new GeneralSecurityException("Failed to build VAPID JWT claims", e);
        }
    }

    private static String originOf(String endpoint) {
        URI uri = URI.create(endpoint);
        StringBuilder origin =
                new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > 0) {
            origin.append(':').append(uri.getPort());
        }
        return origin.toString();
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
