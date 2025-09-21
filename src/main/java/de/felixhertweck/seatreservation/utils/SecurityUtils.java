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
/*
 * Copyright (C) 2025 Felix Hertweck
 *
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
 */
package de.felixhertweck.seatreservation.utils;

import java.security.SecureRandom;

/**
 * Central utility class for secure random number generation. This class is configured for runtime
 * initialization in native builds to avoid issues with SecureRandom seed caching during build time.
 */
public final class SecurityUtils {

    /**
     * Shared SecureRandom instance for all cryptographic operations. This static field requires
     * runtime initialization in native images.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Private constructor to prevent instantiation. */
    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the shared SecureRandom instance.
     *
     * @return the SecureRandom instance
     */
    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }

    /**
     * Generates a cryptographically secure random byte array.
     *
     * @param length the length of the byte array to generate
     * @return a new byte array filled with random bytes
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generates a cryptographically secure random integer within the specified range.
     *
     * @param bound the upper bound (exclusive) for the random number
     * @return a random integer between 0 (inclusive) and bound (exclusive)
     */
    public static int nextInt(int bound) {
        return SECURE_RANDOM.nextInt(bound);
    }

    /**
     * Generates a cryptographically secure random long value.
     *
     * @return a random long value
     */
    public static long nextLong() {
        return SECURE_RANDOM.nextLong();
    }
}
