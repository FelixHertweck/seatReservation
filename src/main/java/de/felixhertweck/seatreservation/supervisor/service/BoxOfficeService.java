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
package de.felixhertweck.seatreservation.supervisor.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.email.service.ReservationEmailContent.BoxOfficeConfirmationContent;
import de.felixhertweck.seatreservation.model.entity.BoxOfficeGuestInfo;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.BoxOfficeGuestInfoRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.service.CheckInTokenService;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeGuestReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.BoxOfficeReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.exception.BookingDeadlineNotPassedException;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BoxOfficeService {

    private static final Logger LOG = Logger.getLogger(BoxOfficeService.class);

    // Shared, passwordless system user reservations are booked under when there is no real
    // account (see db/migration/V8__add_boxoffice_user.sql and import.sql/import-test.sql).
    public static final String BOXOFFICE_USERNAME = "boxoffice";

    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject UserRepository userRepository;
    @Inject SeatRepository seatRepository;
    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;
    @Inject EmailService emailService;
    @Inject LiveViewService liveViewService;
    @Inject UserService userService;

    @Inject CheckInTokenService checkInTokenService;

    @Inject BoxOfficeGuestInfoRepository boxOfficeGuestInfoRepository;

    @Inject EventAuthorizationService eventAuthorizationService;

    /**
     * Returns all users a box office reservation can be created for, excluding the shared {@code
     * boxoffice} system account itself.
     */
    public List<LimitedUserInfoDTO> getUsersForBoxOffice() {
        return userService.getAllUsers().stream()
                .filter(u -> !BOXOFFICE_USERNAME.equalsIgnoreCase(u.username()))
                .toList();
    }

    /**
     * Creates box office reservations for a known, registered user -- the supervisor-accessible
     * equivalent of {@code management.service.ReservationService.createReservations}.
     */
    @Transactional
    public BoxOfficeReservationResponseDTO reserveForKnownUser(
            BoxOfficeReservationRequestDTO dto, AuthenticatedUser currentUser) {
        Event event = loadEvent(dto.getEventId());
        eventAuthorizationService.assertAuthorizedForEvent(currentUser, dto.getEventId());
        assertBookingDeadlinePassed(event);

        User targetUser =
                userRepository
                        .findByIdOptional(dto.getUserId())
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                "User with id " + dto.getUserId() + " not found."));

        CheckInToken checkInToken = checkInTokenService.getOrCreateForUser(targetUser, event);
        List<Reservation> newReservations =
                createReservationsForSeats(
                        event,
                        targetUser,
                        dto.getSeatIds(),
                        dto.isCheckedIn(),
                        dto.isDeductAllowance(),
                        checkInToken);

        reservationRepository.persistAll(newReservations);

        // AuthenticatedUser only carries id/roles (no DB round trip); fetch the acting
        // supervisor's email so they receive a CC, matching
        // management.service.ReservationService.createReservations's behavior for managers.
        User actingUser = userRepository.findById(currentUser.id());
        String ccEmail = actingUser != null ? actingUser.getEmail() : null;
        BoxOfficeConfirmationContent confirmation =
                sendConfirmationSafely(
                        targetUser,
                        newReservations,
                        fullName(targetUser),
                        ccEmail,
                        !dto.isCheckedIn());
        broadcast(event.getId(), newReservations, null);

        LOG.infof(
                "Box office reservation created for known user %s, event %s, %d seat(s) by"
                        + " supervisor %s.",
                targetUser.id, event.getId(), newReservations.size(), currentUser.id());

        return new BoxOfficeReservationResponseDTO(
                targetUser.id, newReservations, confirmation.displayHtml());
    }

    /**
     * Creates box office reservations for a walk-in guest with no account, booked under the shared
     * {@code boxoffice} system user. {@code guestEmail} is used once to send a confirmation and is
     * never persisted.
     */
    @Transactional
    public BoxOfficeReservationResponseDTO reserveForGuest(
            BoxOfficeGuestReservationRequestDTO dto, AuthenticatedUser currentUser) {
        Event event = loadEvent(dto.getEventId());
        eventAuthorizationService.assertAuthorizedForEvent(currentUser, dto.getEventId());
        assertBookingDeadlinePassed(event);

        User boxofficeUser = userRepository.findByUsername(BOXOFFICE_USERNAME);
        if (boxofficeUser == null) {
            LOG.error(
                    "Box office system user not found -- seed data (import.sql / Flyway"
                            + " V8__add_boxoffice_user.sql) is missing.");
            throw new IllegalStateException("Box office system user is not configured.");
        }

        CheckInToken checkInToken = checkInTokenService.createFresh(boxofficeUser, event);
        List<Reservation> newReservations =
                createReservationsForSeats(
                        event,
                        boxofficeUser,
                        dto.getSeatIds(),
                        dto.isCheckedIn(),
                        false,
                        checkInToken);

        reservationRepository.persistAll(newReservations);

        List<BoxOfficeGuestInfo> guestInfos =
                newReservations.stream()
                        .map(r -> new BoxOfficeGuestInfo(r, dto.getGuestName()))
                        .toList();
        boxOfficeGuestInfoRepository.persist(guestInfos);

        String additionalMailAddress =
                dto.getGuestEmail() != null && !dto.getGuestEmail().isBlank()
                        ? dto.getGuestEmail()
                        : null;
        BoxOfficeConfirmationContent confirmation =
                sendConfirmationSafely(
                        boxofficeUser,
                        newReservations,
                        dto.getGuestName(),
                        additionalMailAddress,
                        !dto.isCheckedIn());
        broadcast(event.getId(), newReservations, dto.getGuestName());

        LOG.infof(
                "Box office guest reservation created for event %s, %d seat(s) by supervisor %s.",
                event.getId(), newReservations.size(), currentUser.id());

        return new BoxOfficeReservationResponseDTO(
                boxofficeUser.id, newReservations, confirmation.displayHtml());
    }

    private List<Reservation> createReservationsForSeats(
            Event event,
            User reservationOwner,
            Set<UUID> seatIds,
            boolean checkedIn,
            boolean deductAllowance,
            CheckInToken checkInToken) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ValidationException("No seat IDs provided.");
        }

        Map<UUID, Seat> seatMap =
                seatRepository.findByIds(seatIds.stream().toList()).stream()
                        .collect(Collectors.toMap(s -> s.id, s -> s, (s1, s2) -> s1));

        // Proactive conflict check (mirrors management.service.ReservationService.blockSeats) so
        // a taken seat is rejected before any email/broadcast side effect runs, rather than only
        // surfacing as a late unique-constraint violation at transaction commit.
        List<Reservation> conflicting =
                reservationRepository.findByEventIdAndSeatIds(
                        event.getId(), new ArrayList<>(seatIds));
        if (!conflicting.isEmpty()) {
            UUID conflictingSeatId = conflicting.getFirst().getSeat().id;
            throw new ValidationException(
                    "Seat with id " + conflictingSeatId + " is already reserved or blocked.");
        }

        EventUserAllowance allowance = null;
        if (deductAllowance) {
            allowance =
                    eventUserAllowanceRepository
                            .findByUserAndEvent(reservationOwner, event)
                            .orElseThrow(
                                    () ->
                                            new ValidationException(
                                                    "User has no reservation allowance for this"
                                                            + " event."));
        }

        List<Reservation> newReservations = new ArrayList<>();
        for (UUID seatId : seatIds) {
            Seat seat = seatMap.get(seatId);
            if (seat == null) {
                throw new ValidationException("Seat with id " + seatId + " not found");
            }

            if (deductAllowance) {
                if (allowance.getReservationsAllowedCount() <= 0) {
                    throw new ValidationException(
                            "No more reservations allowed for this user and event.");
                }
                allowance.setReservationsAllowedCount(allowance.getReservationsAllowedCount() - 1);
                eventUserAllowanceRepository.persist(allowance);
            }

            Reservation reservation =
                    new Reservation(
                            reservationOwner,
                            event,
                            seat,
                            Instant.now(),
                            ReservationStatus.RESERVED,
                            checkInToken);
            if (checkedIn) {
                reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);
            }
            newReservations.add(reservation);
        }
        return newReservations;
    }

    private BoxOfficeConfirmationContent sendConfirmationSafely(
            User user,
            List<Reservation> reservations,
            String recipientName,
            String additionalMailAddress,
            boolean includeQrCode) {
        try {
            return emailService.sendBoxOfficeConfirmation(
                    user, reservations, recipientName, additionalMailAddress, includeQrCode);
        } catch (PersistenceException | IllegalStateException e) {
            LOG.errorf(
                    e,
                    "Failed to send box office reservation confirmation email for user ID %s.",
                    user.id);
            return new BoxOfficeConfirmationContent("", "", new byte[0]);
        }
    }

    private void broadcast(UUID eventId, List<Reservation> reservations, String guestName) {
        for (Reservation reservation : reservations) {
            liveViewService.broadcastNewReservation(eventId, reservation, guestName);
        }
    }

    /**
     * Builds a user's display name from first and last name, for the box office confirmation
     * greeting.
     */
    private String fullName(User user) {
        return user.getFirstname() + " " + user.getLastname();
    }

    private Event loadEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            throw new EventNotFoundException("Event with id " + eventId + " not found");
        }
        return event;
    }

    private void assertBookingDeadlinePassed(Event event) {
        Instant deadline = event.getBookingDeadline();
        if (deadline == null || !Instant.now().isAfter(deadline)) {
            throw new BookingDeadlineNotPassedException(
                    "Box office reservations are only available after the event's booking"
                            + " deadline has passed.");
        }
    }
}
