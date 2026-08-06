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
import java.util.Objects;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.GuestSeatAssignment;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationLiveStatus;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.GuestSeatAssignmentRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.supervisor.dto.GuestSeatAssignRequestDTO;
import de.felixhertweck.seatreservation.supervisor.dto.GuestSeatAssignmentResponseDTO;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GuestSeatAssignmentService {

    private static final Logger LOG = Logger.getLogger(GuestSeatAssignmentService.class);

    @Inject GuestSeatAssignmentRepository guestSeatAssignmentRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject EventRepository eventRepository;
    @Inject SeatRepository seatRepository;
    @Inject UserRepository userRepository;
    @Inject LiveViewService liveViewService;

    @Transactional
    public List<GuestSeatAssignmentResponseDTO> assignSeats(
            GuestSeatAssignRequestDTO requestDTO, AuthenticatedUser actor) {
        LOG.infof(
                "Guest seat assignment requested by user %s for event %s with %d seats",
                actor != null ? actor.id() : null, requestDTO.eventId, requestDTO.seatIds.size());

        if (actor != null && !isAuthorizedForEvent(actor, requestDTO.eventId)) {
            throw new SecurityException(
                    "User is not authorized to manage event " + requestDTO.eventId);
        }

        Event event = eventRepository.findById(requestDTO.eventId);
        if (event == null) {
            throw new EventNotFoundException("Event not found with ID: " + requestDTO.eventId);
        }

        User assignedByUser = null;
        if (actor != null) {
            assignedByUser = userRepository.findById(actor.id());
            if (assignedByUser == null) {
                throw new IllegalStateException(
                        "Authenticated user not found in database: " + actor.id());
            }
        }

        List<GuestSeatAssignment> createdAssignments = new ArrayList<>();

        for (UUID seatId : requestDTO.seatIds) {
            Seat seat = seatRepository.findById(seatId);
            if (seat == null) {
                throw new IllegalArgumentException("Seat not found with ID: " + seatId);
            }

            // Check if seat already has an active guest assignment
            if (guestSeatAssignmentRepository.existsByEventIdAndSeatId(
                    requestDTO.eventId, seatId)) {
                throw new IllegalStateException(
                        "Seat " + seatId + " is already assigned to a guest");
            }

            // Check if seat has a registered reservation that is CHECKED_IN or BLOCKED or active
            // (not NO_SHOW/CANCELLED)
            List<Reservation> existingReservations =
                    reservationRepository.findByEventIdAndSeatIds(
                            requestDTO.eventId, List.of(seatId));
            for (Reservation res : existingReservations) {
                if (res.getStatus() == ReservationStatus.BLOCKED) {
                    throw new IllegalStateException("Seat " + seatId + " is blocked");
                }
                if (res.getLiveStatus() == ReservationLiveStatus.CHECKED_IN) {
                    throw new IllegalStateException("Seat " + seatId + " is already checked in");
                }
            }

            GuestSeatAssignment assignment =
                    new GuestSeatAssignment(
                            event, seat, requestDTO.guestName, assignedByUser, Instant.now());
            guestSeatAssignmentRepository.persist(assignment);
            createdAssignments.add(assignment);

            liveViewService.broadcastGuestAssigned(event.getId(), assignment);
        }

        return createdAssignments.stream().map(GuestSeatAssignmentResponseDTO::new).toList();
    }

    @Transactional
    public void removeAssignment(UUID assignmentId, AuthenticatedUser actor) {
        GuestSeatAssignment assignment = guestSeatAssignmentRepository.findById(assignmentId);
        if (assignment == null) {
            throw new ReservationNotFoundException(
                    "Guest assignment not found with ID: " + assignmentId);
        }

        UUID eventId = assignment.getEvent().getId();
        if (actor != null && !isAuthorizedForEvent(actor, eventId)) {
            throw new SecurityException("User is not authorized to manage event " + eventId);
        }

        UUID seatId = assignment.getSeat().getId();
        guestSeatAssignmentRepository.delete(assignment);

        liveViewService.broadcastGuestRemoved(eventId, seatId);
    }

    private boolean isAuthorizedForEvent(AuthenticatedUser user, UUID eventId) {
        if (user == null) return false;
        if (eventRepository.isUserSupervisor(eventId, user.id())) return true;
        Event event = eventRepository.findById(eventId);
        if (event != null
                && event.getManager() != null
                && Objects.equals(event.getManager().id, user.id())) return true;
        return user.isAdmin();
    }
}
