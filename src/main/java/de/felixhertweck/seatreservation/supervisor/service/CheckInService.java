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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.exception.AccessDeniedException;
import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.BoxOfficeGuestInfo;
import de.felixhertweck.seatreservation.model.entity.CheckInToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.BoxOfficeGuestInfoRepository;
import de.felixhertweck.seatreservation.model.repository.CheckInTokenRepository;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.SupervisorEventResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.SupervisorReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.exception.BookingDeadlineNotPassedException;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInException;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInTokenNotFoundException;
import de.felixhertweck.seatreservation.supervisor.exception.EventMismatchException;
import de.felixhertweck.seatreservation.supervisor.exception.UserMismatchException;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import de.felixhertweck.seatreservation.utils.SeatComparators;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CheckInService {

    private static final Logger LOG = Logger.getLogger(CheckInService.class);

    @Inject ReservationRepository reservationRepository;

    @Inject BoxOfficeGuestInfoRepository boxOfficeGuestInfoRepository;

    @Inject CheckInTokenRepository checkInTokenRepository;

    @Inject UserRepository userRepository;

    @Inject EventRepository eventRepository;

    @Inject LiveViewService webSocketService;

    @Inject EventAuthorizationService eventAuthorizationService;

    /**
     * Validates and processes check-in/cancel requests based on a check-in token.
     *
     * @param userId the ID of the user
     * @param eventId the ID of the event
     * @param checkInToken the check-in token string
     * @return list of processed reservations
     * @throws UserMismatchException if the reservation does not belong to the user
     * @throws EventMismatchException if the reservation does not belong to the event
     * @throws CheckInTokenNotFoundException if a check-in token is not found
     */
    @Transactional
    public CheckInInfoResponseDTO getReservationInfos(
            AuthenticatedUser currentUser, UUID userId, UUID eventId, String checkInToken)
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {

        LOG.debugf(
                "Getting reservation infos for targetUser %s, event %s with check-in token %s.",
                userId, eventId, checkInToken);

        if (currentUser != null
                && !eventAuthorizationService.isAuthorizedForEvent(currentUser, eventId)) {
            throw new AccessDeniedException("User is not authorized to access event " + eventId);
        }
        assertBookingDeadlinePassed(loadEvent(eventId));
        List<SupervisorReservationResponseDTO> processedReservations = new ArrayList<>();
        User user = userRepository.findById(userId);

        if (checkInToken != null && !checkInToken.isBlank()) {
            CheckInToken token =
                    checkInTokenRepository
                            .findByToken(checkInToken)
                            .orElseThrow(
                                    () ->
                                            new CheckInTokenNotFoundException(
                                                    String.format(
                                                            "Check-in token %s not found.",
                                                            checkInToken)));

            validateToken(token, userId, eventId);

            List<Reservation> reservations = reservationRepository.findByCheckInToken(token);

            // Sort by seat (row + number) so the check-in UI lists seats in a
            // predictable, human-readable order — especially important for
            // multi-seat Box Office QR codes that group several reservations.
            List<Reservation> sorted =
                    reservations.stream()
                            .filter(r -> r.getStatus() != ReservationStatus.BLOCKED)
                            .sorted(SeatComparators.RESERVATION_COMPARATOR)
                            .toList();

            // Bulk-load guest names for box office reservations (avoids N+1 queries).
            List<UUID> reservationIds = sorted.stream().map(r -> r.id).toList();
            Map<UUID, String> guestNameByReservationId =
                    boxOfficeGuestInfoRepository.findByReservationIdIn(reservationIds).stream()
                            .filter(gi -> gi.getReservation() != null)
                            .collect(
                                    Collectors.toMap(
                                            gi -> gi.getReservation().id,
                                            BoxOfficeGuestInfo::getGuestName,
                                            (a, b) -> a));

            for (Reservation reservation : sorted) {
                LOG.debugf("Processed reservation %s for token %s.", reservation, checkInToken);
                processedReservations.add(
                        new SupervisorReservationResponseDTO(
                                reservation,
                                guestNameByReservationId.get(reservation.id)));
            }
        }

        LOG.debugf(
                "Processed %d reservations for user %s and event %s.",
                processedReservations.size(), userId, eventId);

        return new CheckInInfoResponseDTO(processedReservations, new LimitedUserInfoDTO(user));
    }

    // Backwards-compatible overload for existing tests/usage that provide userId
    @Transactional
    public CheckInInfoResponseDTO getReservationInfos(
            UUID userId, UUID eventId, String checkInToken)
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        // Skip authorization (useful for tests or internal calls) and use userId as the
        // reservation owner id to look up reservations
        return getReservationInfos(null, userId, eventId, checkInToken);
    }

    /**
     * Processes check-in and cancel requests based on reservation IDs. Broadcasts updates to
     * WebSocket clients.
     *
     * @param requestDTO the request DTO containing check-in and cancel IDs, userId, and eventId
     * @throws CheckInException if a reservation ID is not found or does not belong to the
     *     user/event
     */
    @Transactional
    public void processCheckIn(CheckInProcessRequestDTO requestDTO, AuthenticatedUser currentUser)
            throws CheckInException {
        UUID eventId = requestDTO.eventId;
        if (currentUser != null
                && !eventAuthorizationService.isAuthorizedForEvent(currentUser, eventId)) {
            throw new AccessDeniedException("User is not authorized to access event " + eventId);
        }
        assertBookingDeadlinePassed(loadEvent(eventId));
        UUID userId = requestDTO.userId;
        List<UUID> checkInIds = requestDTO.checkIn;
        List<UUID> cancelIds = requestDTO.cancel;

        LOG.debugf(
                "Processing check-in for user %s, event %s with %d check-ins and %d cancellations.",
                userId,
                eventId,
                checkInIds != null ? checkInIds.size() : 0,
                cancelIds != null ? cancelIds.size() : 0);

        if (checkInIds != null && !checkInIds.isEmpty()) {
            List<Reservation> checkInReservations =
                    reservationRepository.findAllByIdUserIdAndEventId(checkInIds, userId, eventId);
            Map<UUID, Reservation> reservationMap =
                    checkInReservations.stream()
                            .collect(
                                    Collectors.toMap(
                                            r -> r.id, Function.identity(), (r1, r2) -> r1));

            for (UUID reservationId : checkInIds) {
                Reservation reservation = reservationMap.get(reservationId);

                if (reservation == null) {
                    LOG.warnf(
                            "Reservation with ID %s not found or does not belong to user %s/event"
                                    + " %s for check-in.",
                            reservationId, userId, eventId);
                    throw new CheckInException(
                            String.format(
                                    "Reservation with ID %s not found or does not belong to user"
                                            + " %s/event %s for check-in.",
                                    reservationId, userId, eventId));
                }

                LOG.debugf("Setting reservation %s to CHECK_IN status.", reservationId);
                reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);
                LOG.infof("Reservation %s successfully checked in.", reservationId);
            }

            reservationRepository.persistAll(checkInReservations);

            for (Reservation reservation : checkInReservations) {
                // Broadcast check-in update to WebSocket clients
                webSocketService.broadcastUpdate(reservation.getEvent().getId(), reservation);
            }
        }

        if (cancelIds != null && !cancelIds.isEmpty()) {
            List<Reservation> cancelReservations =
                    reservationRepository.findAllByIdUserIdAndEventId(cancelIds, userId, eventId);
            Map<UUID, Reservation> reservationMap =
                    cancelReservations.stream()
                            .collect(
                                    Collectors.toMap(
                                            r -> r.id, Function.identity(), (r1, r2) -> r1));

            for (UUID reservationId : cancelIds) {
                Reservation reservation = reservationMap.get(reservationId);

                if (reservation == null) {
                    LOG.warnf(
                            "Reservation with ID %s not found or does not belong to user %s/event"
                                    + " %s for cancellation.",
                            reservationId, userId, eventId);
                    throw new CheckInException(
                            String.format(
                                    "Reservation with ID %s not found or does not belong to user"
                                            + " %s/event %s for cancellation.",
                                    reservationId, userId, eventId));
                }

                LOG.debugf("Setting reservation %s to CANCEL status.", reservationId);
                reservation.setLiveStatus(ReservationLiveStatus.CANCELLED);
                LOG.infof("Reservation %s successfully cancelled.", reservationId);
            }

            reservationRepository.persistAll(cancelReservations);

            for (Reservation reservation : cancelReservations) {
                // Broadcast cancellation update to WebSocket clients
                webSocketService.broadcastUpdate(reservation.getEvent().getId(), reservation);
            }
        }

        LOG.debugf(
                "Check-in processing completed for user %s, event %s with %d check-ins and %d"
                        + " cancellations.",
                userId,
                eventId,
                checkInIds != null ? checkInIds.size() : 0,
                cancelIds != null ? cancelIds.size() : 0);
    }

    // Backwards-compatible overload
    @Transactional
    public void processCheckIn(CheckInProcessRequestDTO requestDTO) throws CheckInException {
        // Backwards-compatible overload without a acting user: skip authorization
        processCheckIn(requestDTO, null);
    }

    /**
     * Retrieves a list of all events for the supervisor view.
     *
     * @return A list of SupervisorEventResponseDTO.
     */
    @Transactional
    public List<SupervisorEventResponseDTO> getAllEventsForSupervisor(
            AuthenticatedUser currentUser) {
        LOG.debug("Retrieving all events for supervisor view.");
        if (currentUser == null) {
            return eventRepository.findAll().stream()
                    .map(SupervisorEventResponseDTO::new)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        Stream<Event> authorizedEvents =
                currentUser.isAdmin()
                        ? eventRepository.findAll().stream()
                        : eventRepository
                                .findAuthorizedEvents(userRepository.getReference(currentUser.id()))
                                .stream();

        return authorizedEvents
                .map(SupervisorEventResponseDTO::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    // Backwards-compatible overload
    @Transactional
    public List<SupervisorEventResponseDTO> getAllEventsForSupervisor() {
        return eventRepository.findAll().stream()
                .map(SupervisorEventResponseDTO::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Retrieves a list of all usernames that have an active reservation for a specific event.
     *
     * @param eventId the ID of the event
     * @return A list of strings, where each string is a username.
     */
    @Transactional
    public List<String> getUsernamesWithReservations(AuthenticatedUser currentUser, UUID eventId) {
        LOG.debugf("Retrieving usernames with reservations for event %s.", eventId);
        if (currentUser != null
                && !eventAuthorizationService.isAuthorizedForEvent(currentUser, eventId)) {
            throw new AccessDeniedException("User is not authorized to access event " + eventId);
        }
        return reservationRepository.findDistinctUsernamesByEventId(eventId);
    }

    // Backwards-compatible overload
    public List<String> getUsernamesWithReservations(UUID eventId) {
        return getUsernamesWithReservations(null, eventId);
    }

    /**
     * Retrieves check-in information for a given username.
     *
     * @param username the username of the user
     * @return CheckInInfoResponseDTO containing the user's reservations
     * @throws ReservationNotFoundException if no reservations are found for the user
     */
    @Transactional
    public CheckInInfoResponseDTO getReservationInfosByUsername(
            AuthenticatedUser currentUser, String username) throws ReservationNotFoundException {
        LOG.debug("Getting reservation infos for user.");

        User user = userRepository.findByUsername(username);
        if (user == null) {
            LOG.warn("User with specified username not found.");
            throw new ReservationNotFoundException("User with specified username not found.");
        }

        List<Reservation> reservations = reservationRepository.findByUserWithDetails(user);
        if (reservations.isEmpty()) {
            LOG.warnf("No reservations found for user %s.", username);
            throw new ReservationNotFoundException(
                    String.format("No reservations found for user %s.", username));
        }

        Map<UUID, Boolean> authorizationCache = new HashMap<>();

        // Sort by seat so the check-in UI always lists seats in a predictable order.
        List<Reservation> filtered =
                reservations.stream()
                        .filter(
                                r ->
                                        currentUser == null
                                                || authorizationCache.computeIfAbsent(
                                                        r.getEvent().getId(),
                                                        eventId ->
                                                                eventAuthorizationService
                                                                        .isAuthorizedForEvent(
                                                                                currentUser,
                                                                                eventId)))
                        .sorted(SeatComparators.RESERVATION_COMPARATOR)
                        .toList();

        // Bulk-load guest names for any box office reservations in the result.
        List<UUID> reservationIds = filtered.stream().map(r -> r.id).toList();
        Map<UUID, String> guestNameByReservationId =
                boxOfficeGuestInfoRepository.findByReservationIdIn(reservationIds).stream()
                        .filter(gi -> gi.getReservation() != null)
                        .collect(
                                Collectors.toMap(
                                        gi -> gi.getReservation().id,
                                        BoxOfficeGuestInfo::getGuestName,
                                        (a, b) -> a));

        List<SupervisorReservationResponseDTO> processedReservations =
                filtered.stream()
                        .map(
                                r ->
                                        new SupervisorReservationResponseDTO(
                                                r, guestNameByReservationId.get(r.id)))
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        LOG.debugf(
                "Processed %d reservations for user %s.", processedReservations.size(), username);

        return new CheckInInfoResponseDTO(processedReservations, new LimitedUserInfoDTO(user));
    }

    private void validateToken(CheckInToken token, UUID userId, UUID eventId)
            throws UserMismatchException, EventMismatchException {
        if (!Objects.equals(token.getUser().id, userId)) {
            throw new UserMismatchException(
                    String.format(
                            "Check-in token %s does not belong to user %s.",
                            token.getToken(), userId));
        }
        if (!Objects.equals(token.getEvent().id, eventId)) {
            throw new EventMismatchException(
                    String.format(
                            "Check-in token %s does not belong to event %s.",
                            token.getToken(), eventId));
        }
    }

    private Event loadEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            throw new EventNotFoundException("Event with id " + eventId + " not found");
        }
        return event;
    }

    /** Mirrors BoxOfficeService/LiveViewService's private assertBookingDeadlinePassed check. */
    private void assertBookingDeadlinePassed(Event event) {
        Instant deadline = event.getBookingDeadline();
        if (deadline == null || !Instant.now().isAfter(deadline)) {
            throw new BookingDeadlineNotPassedException(
                    "Check-in is only available after the event's booking deadline has passed.");
        }
    }
}
