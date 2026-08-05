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
package de.felixhertweck.seatreservation.security.exceptions;

import java.time.Instant;

/** Exception thrown when an email resend endpoint is requested while on cooldown. */
public class EmailCooldownException extends RuntimeException {

    private final Instant retryAfter;

    public EmailCooldownException(String message, Instant retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Instant getRetryAfter() {
        return retryAfter;
    }
}
