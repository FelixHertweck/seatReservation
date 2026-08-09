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
package de.felixhertweck.seatreservation.supervisor.dto;

import java.util.List;
import java.util.UUID;

import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response for a box office reservation creation call.
 *
 * <p>{@code reservationUserId} is the id that must be embedded in the QR/print/email check-in
 * payload (built as {@code "<reservationUserId>;<eventId>;<checkInToken>"}, matching {@code
 * EmailService.generateQrCodeContent} and the check-in scanner) -- the target user's id for a
 * "known user" reservation, or the shared {@code boxoffice} system user's id for a guest
 * reservation.
 *
 * <p>{@code confirmationHtml} is the server-rendered "Abendkasse" confirmation (see {@code
 * EmailService.sendBoxOfficeConfirmation}), returned regardless of whether an email was actually
 * sent, so the frontend can always offer a print copy identical to the emailed confirmation.
 */
@RegisterForReflection
public record BoxOfficeReservationResponseDTO(
        UUID eventId,
        UUID reservationUserId,
        List<BoxOfficeSeatDTO> seats,
        String confirmationHtml) {

    @RegisterForReflection
    public record BoxOfficeSeatDTO(
            UUID seatId, String seatNumber, String checkInToken, ReservationLiveStatus liveStatus) {
        public BoxOfficeSeatDTO(Reservation reservation) {
            this(
                    reservation.getSeat().getId(),
                    reservation.getSeat().getSeatNumber(),
                    reservation.getCheckInToken() != null
                            ? reservation.getCheckInToken().getToken()
                            : null,
                    reservation.getLiveStatus());
        }
    }

    public BoxOfficeReservationResponseDTO(
            UUID reservationUserId, List<Reservation> reservations, String confirmationHtml) {
        this(
                reservations.isEmpty() ? null : reservations.getFirst().getEvent().getId(),
                reservationUserId,
                reservations.stream().map(BoxOfficeSeatDTO::new).toList(),
                confirmationHtml);
    }
}
