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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.GuestSeatAssignment;
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
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class GuestSeatAssignmentServiceTest {

    @Inject GuestSeatAssignmentService guestSeatAssignmentService;

    @InjectMock GuestSeatAssignmentRepository guestSeatAssignmentRepository;
    @InjectMock ReservationRepository reservationRepository;
    @InjectMock EventRepository eventRepository;
    @InjectMock SeatRepository seatRepository;
    @InjectMock UserRepository userRepository;
    @InjectMock LiveViewService liveViewService;

    private final UUID eventId = id(1);
    private final UUID seatId = id(2);
    private final UUID userId = id(3);
    private final AuthenticatedUser adminUser =
            new AuthenticatedUser(userId, java.util.Set.of("ADMIN"));

    @BeforeEach
    public void setUp() {
        Mockito.reset(
                guestSeatAssignmentRepository,
                reservationRepository,
                eventRepository,
                seatRepository,
                userRepository,
                liveViewService);
    }

    // ---- assignSeats tests ----

    @Test
    public void testAssignSeats_EventNotFound() {
        GuestSeatAssignRequestDTO request =
                new GuestSeatAssignRequestDTO(eventId, List.of(seatId), "Max");
        when(eventRepository.findById(eventId)).thenReturn(null);

        assertThrows(
                EventNotFoundException.class,
                () -> guestSeatAssignmentService.assignSeats(request, adminUser));
    }

    @Test
    public void testAssignSeats_UserNotFoundInDb() {
        GuestSeatAssignRequestDTO request =
                new GuestSeatAssignRequestDTO(eventId, List.of(seatId), "Max");
        Event event = new Event();
        event.id = eventId;

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(userRepository.findById(userId)).thenReturn(null); // user not in DB

        assertThrows(
                IllegalStateException.class,
                () -> guestSeatAssignmentService.assignSeats(request, adminUser));
    }

    @Test
    public void testAssignSeats_AlreadyAssignedToGuest() {
        GuestSeatAssignRequestDTO request =
                new GuestSeatAssignRequestDTO(eventId, List.of(seatId), "Max");
        Event event = new Event();
        event.id = eventId;
        EventLocation location = new EventLocation();
        location.id = id(10);
        Seat seat = new Seat("1", "A", location);
        seat.id = seatId;
        User user = new User();
        user.id = userId;

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(userRepository.findById(userId)).thenReturn(user);
        when(seatRepository.findById(seatId)).thenReturn(seat);
        when(guestSeatAssignmentRepository.existsByEventIdAndSeatId(eventId, seatId))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> guestSeatAssignmentService.assignSeats(request, adminUser));
    }

    @Test
    public void testAssignSeats_Success() {
        GuestSeatAssignRequestDTO request =
                new GuestSeatAssignRequestDTO(eventId, List.of(seatId), "Max Mustermann");
        Event event = new Event();
        event.id = eventId;
        EventLocation location = new EventLocation();
        location.id = id(10);
        Seat seat = new Seat("1", "A", location);
        seat.id = seatId;
        User user = new User();
        user.id = userId;

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(userRepository.findById(userId)).thenReturn(user);
        when(seatRepository.findById(seatId)).thenReturn(seat);
        when(guestSeatAssignmentRepository.existsByEventIdAndSeatId(eventId, seatId))
                .thenReturn(false);
        when(reservationRepository.findByEventIdAndSeatIds(eventId, List.of(seatId)))
                .thenReturn(List.of());

        List<GuestSeatAssignmentResponseDTO> results =
                guestSeatAssignmentService.assignSeats(request, adminUser);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Max Mustermann", results.get(0).guestName());

        verify(guestSeatAssignmentRepository).persist(any(GuestSeatAssignment.class));
        verify(liveViewService).broadcastGuestAssigned(eq(eventId), any(GuestSeatAssignment.class));
    }

    // ---- removeAssignment tests ----

    @Test
    public void testRemoveAssignment_NotFound() {
        UUID assignmentId = id(99);
        when(guestSeatAssignmentRepository.findById(assignmentId)).thenReturn(null);

        assertThrows(
                ReservationNotFoundException.class,
                () -> guestSeatAssignmentService.removeAssignment(assignmentId, adminUser));
    }

    @Test
    public void testRemoveAssignment_Success() {
        UUID assignmentId = id(99);
        Event event = new Event();
        event.id = eventId;
        EventLocation location = new EventLocation();
        location.id = id(10);
        Seat seat = new Seat("1", "A", location);
        seat.id = seatId;
        // assignedBy can be null for this test (testing the remove path, not assign)
        GuestSeatAssignment assignment =
                new GuestSeatAssignment(event, seat, "Max Mustermann", null, Instant.now());
        assignment.id = assignmentId;

        when(guestSeatAssignmentRepository.findById(assignmentId)).thenReturn(assignment);
        // adminUser.isAdmin() => true, so authorization passes without further DB calls
        when(eventRepository.isUserSupervisor(eventId, userId)).thenReturn(false);
        when(eventRepository.findById(eventId)).thenReturn(event);

        guestSeatAssignmentService.removeAssignment(assignmentId, adminUser);

        verify(guestSeatAssignmentRepository).delete(assignment);
        verify(liveViewService).broadcastGuestRemoved(eq(eventId), eq(seatId));
    }
}
