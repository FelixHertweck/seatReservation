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
package de.felixhertweck.seatreservation.model.entity;

/**
 * Lifecycle state of an {@link OutboundEmail} in the transactional email outbox.
 *
 * <pre>
 *   PENDING  --(dispatcher claims)-->  SENDING  --(SMTP ok)-->  SENT
 *      ^                                   |
 *      |------(retry, attempts left)-------|
 *                                          |
 *                                          +--(attempts exhausted)--> FAILED
 * </pre>
 */
public enum EmailStatus {
    /** Waiting to be picked up by the dispatcher (or waiting for its next retry). */
    PENDING,
    /** Currently claimed by the dispatcher and being handed to the mail server. */
    SENDING,
    /** Successfully accepted by the mail server. */
    SENT,
    /** Permanently failed after exhausting all retry attempts (dead letter). */
    FAILED
}
