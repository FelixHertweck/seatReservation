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

import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.CheckInInfoResponseDTO;
import de.felixhertweck.seatreservation.supervisor.dto.LiveReservationResponseDTO;
import de.felixhertweck.seatreservation.supervisor.exception.CheckInTokenNotFoundException;
import de.felixhertweck.seatreservation.supervisor.exception.EventMismatchException;
import de.felixhertweck.seatreservation.supervisor.exception.UserMismatchException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CheckInService {

    private static final Logger LOG = Logger.getLogger(CheckInService.class);

    @Inject ReservationRepository reservationRepository;

    @Inject UserRepository userRepository;

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
            Long userId, Long eventId, List<String> checkInTokens)
            throws UserMismatchException, EventMismatchException, CheckInTokenNotFoundException {

        LOG.debugf(
                "Getting reservation infos for user %d, event %d with %d check-in tokens.",
                (Object) userId,
                (Object) eventId,
                (Object) (checkInTokens != null ? checkInTokens.size() : 0));

        List<LiveReservationResponseDTO> processedReservations = new ArrayList<>();
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
                LOG.infof(
                        "Reservation %s for user %d and event %d checked in.",
                        token, userId, eventId);

                processedReservations.add(new LiveReservationResponseDTO(reservation));
            }
        }

        LOG.debugf(
                "Processed %d reservations for user %d and event %d.",
                processedReservations.size(), userId, eventId);

        return new CheckInInfoResponseDTO(processedReservations, new UserDTO(user));
    }

    /**
     * Processes check-in and cancel requests based on reservation IDs. Broadcasts updates to
     * WebSocket clients.
     *
     * @param checkInIds list of reservation IDs for check-in
     * @param cancelIds list of reservation IDs for cancellation
     * @throws ReservationNotFoundException if a reservation ID is not found
     */
    @Transactional
    public void processCheckIn(List<Long> checkInIds, List<Long> cancelIds)
            throws ReservationNotFoundException {
        LOG.debugf(
                "Processing check-in with %d check-ins and %d cancellations.",
                checkInIds != null ? checkInIds.size() : 0,
                cancelIds != null ? cancelIds.size() : 0);

        if (checkInIds != null && !checkInIds.isEmpty()) {
            for (Long reservationId : checkInIds) {
                Optional<Reservation> reservationOptional =
                        reservationRepository.findByIdOptional(reservationId);

                if (!reservationOptional.isPresent()) {
                    LOG.warnf("Reservation with ID %d not found for check-in.", reservationId);
                    throw new ReservationNotFoundException(
                            String.format(
                                    "Reservation with ID %d not found for check-in.",
                                    reservationId));
                }

                Reservation reservation = reservationOptional.get();
                LOG.debugf("Setting reservation %d to CHECK_IN status.", reservationId);
                reservation.setLiveStatus(ReservationLiveStatus.CHECKED_IN);
                reservationRepository.persist(reservation);
                LOG.infof("Reservation %d successfully checked in.", reservationId);

                // Broadcast check-in update to WebSocket clients
                LiveReservationResponseDTO dto = new LiveReservationResponseDTO(reservation);
                webSocketService.broadcastCheckInUpdate(reservation.getEvent().getId(), dto);
            }
        }

        if (cancelIds != null && !cancelIds.isEmpty()) {
            for (Long reservationId : cancelIds) {
                Optional<Reservation> reservationOptional =
                        reservationRepository.findByIdOptional(reservationId);

                if (reservationOptional.isPresent()) {
                    Reservation reservation = reservationOptional.get();
                    LOG.debugf("Setting reservation %d to CANCEL status.", reservationId);
                    reservation.setLiveStatus(ReservationLiveStatus.CANCELLED);
                    reservationRepository.persist(reservation);
                    LOG.infof("Reservation %d successfully cancelled.", reservationId);

                    // Broadcast cancellation update to WebSocket clients
                    LiveReservationResponseDTO dto = new LiveReservationResponseDTO(reservation);
                    webSocketService.broadcastCancellationUpdate(
                            reservation.getEvent().getId(), dto);
                } else {
                    LOG.warnf("Reservation with ID %d not found for cancellation.", reservationId);
                }
            }
        }

        LOG.debugf(
                "Check-in processing completed for %d check-ins and %d cancellations.",
                checkInIds != null ? checkInIds.size() : 0,
                cancelIds != null ? cancelIds.size() : 0);
    }

    /**
     * Retrieves a list of all usernames that have an active reservation.
     *
     * @return A list of strings, where each string is a username.
     */
    @Transactional
    public List<String> getUsernamesWithReservations() {
        LOG.debug("Retrieving all usernames with reservations.");
        return reservationRepository.findAll().stream()
                .map(Reservation::getUser)
                .filter(Objects::nonNull)
                .map(User::getUsername)
                .distinct()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Retrieves check-in information for a given username.
     *
     * @param username the username of the user
     * @return CheckInInfoResponseDTO containing the user's reservations
     * @throws ReservationNotFoundException if no reservations are found for the user
     */
    @Transactional
    public CheckInInfoResponseDTO getReservationInfosByUsername(String username)
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

        List<LiveReservationResponseDTO> processedReservations =
                reservations.stream()
                        .map(LiveReservationResponseDTO::new)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        LOG.debugf(
                "Processed %d reservations for user %s.", processedReservations.size(), username);

        return new CheckInInfoResponseDTO(processedReservations, new UserDTO(user));
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
}
