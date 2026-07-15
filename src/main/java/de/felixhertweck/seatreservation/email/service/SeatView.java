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
package de.felixhertweck.seatreservation.email.service;

/**
 * Lightweight view of a seat for use in email templates. Decouples the Qute templates from the JPA
 * {@code Seat} entity and captures only the fields the templates render.
 *
 * @param number the seat number (e.g. {@code A1})
 * @param row the seat row label
 * @param area the seat's area label
 */
public record SeatView(String number, String row, String area) {}
