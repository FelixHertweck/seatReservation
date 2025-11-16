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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInProcessRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.SupervisorEventResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.SupervisorReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInException;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInTokenNotFoundException;
import de.felixhertweck.seatreservation.supervisor.exception.EventMismatchException;
import de.felixhertweck.seatreservation.supervisor.exception.UserMismatchException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CheckInService {

    private static final Logger LOG = Logger.getLogger(CheckInService.class);

    @Inject ReservationRepository reservationRepository;

    @Inject UserRepository userRepository;

    @Inject EventRepository eventRepository;

    @Inject LiveViewService webSocketService;

    /**
     * Validates and processes check-in/cancel requests based on reservation tokens.
     *
     * @param userId the ID of the user
     * @param eventId the ID of the event
     * @param checkInTokens list of check-in tokens
     * @return list of processed reservations
     * @throws UserMismatchException if the reservation does not belong to the user
     * @throws EventMismatchException if the reservation does not belong to the event
     * @throws CheckInTokenNotFoundException if a check-in token is not found
     */
    @Transactional
    public CheckInInfoResponseDTO getReservationInfos(
            User currentUser, Long userId, Long eventId, List<String> checkInTokens)
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {

        LOG.debugf(
                "Getting reservation infos for targetUser %d, event %d with %d check-in tokens.",
                (Object) userId,
                (Object) eventId,
                (Object) (checkInTokens != null ? checkInTokens.size() : 0));

        if (currentUser != null && !isAuthorizedForEvent(currentUser, eventId)) {
            throw new SecurityException("User is not authorized to access event " + eventId);
        }
        List<SupervisorReservationResponseDTO> processedReservations = new ArrayList<>();
        User user = userRepository.findById(userId);

        if (checkInTokens != null && !checkInTokens.isEmpty()) {
            for (String token : checkInTokens) {
                Optional<Reservation> reservationOptional =
                        reservationRepository.findByCheckInCode(token);

                if (!reservationOptional.isPresent()) {
                    LOG.warnf("Check-in token %s not found.", token);
                    throw new CheckInTokenNotFoundException(
                            String.format("Check-in token %s not found.", token));
                }

                Reservation reservation = reservationOptional.get();
                validateReservation(reservation, userId, eventId);
                LOG.debugf("Processed reservation %s for token %s.", reservation, token);

                processedReservations.add(new SupervisorReservationResponseDTO(reservation));
            }
        }

        LOG.debugf(
                "Processed %d reservations for user %d and event %d.",
                processedReservations.size(), userId, eventId);

        return new CheckInInfoResponseDTO(processedReservations, new LimitedUserInfoDTO(user));
    }

    // Backwards-compatible overload for existing tests/usage that provide userId
    @Transactional
    public CheckInInfoResponseDTO getReservationInfos(
            Long userId, Long eventId, List<String> checkInTokens)
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {
        // Skip authorization (useful for tests or internal calls) and use userId as the
        // reservation owner id to look up reservations
        return getReservationInfos(null, userId, eventId, checkInTokens);
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
    public void processCheckIn(CheckInProcessRequestDTO requestDTO, User currentUser)
            throws CheckInException {
        Long eventId = requestDTO.eventId;
        if (currentUser != null && !isAuthorizedForEvent(currentUser, eventId)) {
            throw new SecurityException("User is not authorized to access event " + eventId);
        }
        Long userId = requestDTO.userId;
        List<Long> checkInIds = requestDTO.checkIn;
        List<Long> cancelIds = requestDTO.cancel;

        LOG.debugf(
                "Processing check-in for user %d, event %d with %d check-ins and %d cancellations.",
                userId,
                eventId,
                checkInIds != null ? checkInIds.size() : 0,
                cancelIds != null ? cancelIds.size() : 0);

        if (checkInIds != null && !checkInIds.isEmpty()) {
            for (Long reservationId : checkInIds) {
                Optional<Reservation> reservationOptional =
                        reservationRepository.findByIdUserIdAndEventId(
                                reservationId, userId, eventId);

                if (!reservationOptional.isPresent()) {
                    LOG.warnf(
                            "Reservation with ID %d not found or does not belong to user %d/event"
                                    + " %d for check-in.",
                            reservationId, userId, eventId);
                    throw new CheckInException(
                            String.format(
                                    "Reservation with ID %d not found or does not belong to user"
                                            + " %d/event %d for check-in.",
                                    reservationId, userId, eventId));
                }

                Reservation reservation = reservationOptional.get();

                LOG.debugf("Setting reservation %d to CHECK_IN status.", reservationId);
                reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);
                reservationRepository.persist(reservation);
                LOG.infof("Reservation %d successfully checked in.", reservationId);

                // Broadcast check-in update to WebSocket clients
                webSocketService.broadcastUpdate(reservation.getEvent().getId(), reservation);
            }
        }

        if (cancelIds != null && !cancelIds.isEmpty()) {
            for (Long reservationId : cancelIds) {
                Optional<Reservation> reservationOptional =
                        reservationRepository.findByIdUserIdAndEventId(
                                reservationId, userId, eventId);

                if (!reservationOptional.isPresent()) {
                    LOG.warnf(
                            "Reservation with ID %d not found or does not belong to user %d/event"
                                    + " %d for cancellation.",
                            reservationId, userId, eventId);
                    throw new CheckInException(
                            String.format(
                                    "Reservation with ID %d not found or does not belong to user"
                                            + " %d/event %d for cancellation.",
                                    reservationId, userId, eventId));
                }

                Reservation reservation = reservationOptional.get();

                LOG.debugf("Setting reservation %d to CANCEL status.", reservationId);
                reservation.setLiveStatus(ReservationLiveStatus.CANCELLED);
                reservationRepository.persist(reservation);
                LOG.infof("Reservation %d successfully cancelled.", reservationId);

                // Broadcast cancellation update to WebSocket clients
                webSocketService.broadcastUpdate(reservation.getEvent().getId(), reservation);
            }
        }

        LOG.debugf(
                "Check-in processing completed for user %d, event %d with %d check-ins and %d"
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
    public List<SupervisorEventResponseDTO> getAllEventsForSupervisor(User currentUser) {
        LOG.debug("Retrieving all events for supervisor view.");
        if (currentUser == null) {
            return eventRepository.findAll().stream()
                    .map(SupervisorEventResponseDTO::new)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return eventRepository.findAll().stream()
                .filter(e -> isAuthorizedForEvent(currentUser, e.getId()))
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
    public List<String> getUsernamesWithReservations(User currentUser, Long eventId) {
        LOG.debugf("Retrieving usernames with reservations for event %d.", eventId);
        if (currentUser != null && !isAuthorizedForEvent(currentUser, eventId)) {
            throw new SecurityException("User is not authorized to access event " + eventId);
        }
        return reservationRepository.find("event.id", eventId).stream()
                .filter(r -> r.getStatus() != ReservationStatus.BLOCKED)
                .map(Reservation::getUser)
                .filter(Objects::nonNull)
                .map(User::getUsername)
                .distinct()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    // Backwards-compatible overload
    public List<String> getUsernamesWithReservations(Long eventId) {
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
    public CheckInInfoResponseDTO getReservationInfosByUsername(User currentUser, String username)
            throws ReservationNotFoundException {
        LOG.debugf("Getting reservation infos for username %s.", username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            LOG.warnf("User with username %s not found.", username);
            throw new ReservationNotFoundException(
                    String.format("User with username %s not found.", username));
        }

        List<Reservation> reservations = reservationRepository.findByUser(user);
        if (reservations.isEmpty()) {
            LOG.warnf("No reservations found for user %s.", username);
            throw new ReservationNotFoundException(
                    String.format("No reservations found for user %s.", username));
        }

        List<SupervisorReservationResponseDTO> processedReservations =
                reservations.stream()
                        .filter(
                                r ->
                                        currentUser == null
                                                || isAuthorizedForEvent(
                                                        currentUser, r.getEvent().getId()))
                        .map(SupervisorReservationResponseDTO::new)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        LOG.debugf(
                "Processed %d reservations for user %s.", processedReservations.size(), username);

        return new CheckInInfoResponseDTO(processedReservations, new LimitedUserInfoDTO(user));
    }

    // Backwards-compatible overload
    public CheckInInfoResponseDTO getReservationInfosByUsername(String username)
            throws ReservationNotFoundException {
        // Fallback to no filtering
        return getReservationInfosByUsername(null, username);
    }

    private void validateReservation(Reservation reservation, Long userId, Long eventId)
            throws UserMismatchException, EventMismatchException {
        if (!Objects.equals(reservation.getUser().id, userId)) {
            throw new UserMismatchException(
                    String.format(
                            "Reservation %s does not belong to user %d.",
                            reservation.getCheckInCode(), userId));
        }
        if (!Objects.equals(reservation.getEvent().id, eventId)) {
            throw new EventMismatchException(
                    String.format(
                            "Reservation %s does not belong to event %d.",
                            reservation.getCheckInCode(), eventId));
        }
        if (reservation.getStatus() == ReservationStatus.BLOCKED) {
            throw new IllegalStateException(
                    String.format("Reservation %s is blocked.", reservation.getCheckInCode()));
        }
    }

    private boolean isAuthorizedForEvent(User user, Long eventId) {
        if (user == null || eventId == null) return false;
        // If user is a supervisor for the event
        if (eventRepository.isUserSupervisor(eventId, user.id)) return true;
        // If user is manager for the event
        Event event = eventRepository.findById(eventId);
        if (event != null
                && event.getManager() != null
                && Objects.equals(event.getManager().id, user.id)) {
            return true;
        }
        // If user is admin
        return user.getRoles() != null && user.getRoles().contains(Roles.ADMIN);
    }
}
