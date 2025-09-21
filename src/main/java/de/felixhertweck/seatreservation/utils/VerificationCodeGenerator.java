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
package de.felixhertweck.seatreservation.utils;

/** Utility class for generating 6-digit verification codes. */
public class VerificationCodeGenerator {
    private static final int MIN_CODE = 100000; // 6-digit minimum
    private static final int MAX_CODE = 999999; // 6-digit maximum

    /**
     * Generates a 6-digit random verification code.
     *
     * @return A 6-digit verification code as a String
     */
    public static String generate() {
        int code = SecurityUtils.getSecureRandom().nextInt(MAX_CODE - MIN_CODE + 1) + MIN_CODE;
        return String.valueOf(code);
    }
}
