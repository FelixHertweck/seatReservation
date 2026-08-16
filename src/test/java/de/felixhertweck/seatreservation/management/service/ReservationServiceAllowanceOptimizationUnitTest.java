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
package de.felixhertweck.seatreservation.management.service;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.management.dto.ReservationRequestDTO;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.ReservationStatus;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.reservation.service.CheckInTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceAllowanceOptimizationUnitTest {

    @Mock ReservationRepository reservationRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock SeatRepository seatRepository;
    @Mock EventUserAllowanceRepository eventUserAllowanceRepository;
    @Mock EmailService emailService;
    @Mock CheckInTokenService checkInTokenService;
    @Mock EventAccessService eventAccessService;

    @InjectMocks ReservationService reservationService;

    private User managerUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        managerUser = new User();
        managerUser.id = id(1);
        managerUser.setEmail("manager@example.com");

        testEvent = new Event();
        testEvent.id = id(2);
        testEvent.setName("Test Event");
    }

    @Test
    void testDeleteReservation_BatchPersistVerification() throws Exception {
        // Create 2 users, each with 5 deleted reservations (total 10 reservations)
        User user1 = new User();
        user1.id = id(10);
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.id = id(20);
        user2.setEmail("user2@example.com");

        EventUserAllowance allowance1 = new EventUserAllowance(user1, testEvent, 5);
        allowance1.id = id(1001);
        EventUserAllowance allowance2 = new EventUserAllowance(user2, testEvent, 5);
        allowance2.id = id(1002);

        List<UUID> reservationIds = new ArrayList<>();
        List<Reservation> reservations = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            UUID resId1 = id(100 + i);
            reservationIds.add(resId1);
            Reservation r1 =
                    new Reservation(
                            user1,
                            testEvent,
                            null,
                            Instant.now(),
                            ReservationStatus.RESERVED,
                            null);
            r1.id = resId1;
            reservations.add(r1);

            UUID resId2 = id(200 + i);
            reservationIds.add(resId2);
            Reservation r2 =
                    new Reservation(
                            user2,
                            testEvent,
                            null,
                            Instant.now(),
                            ReservationStatus.RESERVED,
                            null);
            r2.id = resId2;
            reservations.add(r2);
        }

        when(reservationRepository.findByIds(reservationIds)).thenReturn(reservations);
        when(eventAccessService.isManager(eq(testEvent), any())).thenReturn(true);
        when(eventUserAllowanceRepository.findByEventAndUserIds(eq(testEvent), any()))
                .thenReturn(List.of(allowance1, allowance2));

        reservationService.deleteReservation(reservationIds, managerUser);

        // Verify total allowance increments
        assertEquals(10, allowance1.getReservationsAllowedCount()); // 5 + 5
        assertEquals(10, allowance2.getReservationsAllowedCount()); // 5 + 5

        // Verify single batch persist call for updated allowances, instead of 10 individual calls
        verify(eventUserAllowanceRepository, never()).persist(any(EventUserAllowance.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<EventUserAllowance>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(eventUserAllowanceRepository, times(1)).persist(captor.capture());

        List<EventUserAllowance> persistedList = new ArrayList<>();
        captor.getValue().forEach(persistedList::add);
        assertEquals(2, persistedList.size());
    }

    @Test
    void testCreateReservations_DeductAllowancePersistsOnceAfterLoop() throws Exception {
        User targetUser = new User();
        targetUser.id = id(30);
        targetUser.setEmail("target@example.com");

        EventUserAllowance allowance = new EventUserAllowance(targetUser, testEvent, 10);
        allowance.id = id(1003);

        Set<UUID> seatIds =
                new HashSet<>(List.of(id(1001), id(1002), id(1003), id(1004), id(1005)));
        List<Seat> seats = new ArrayList<>();
        for (UUID seatId : seatIds) {
            Seat seat = mock(Seat.class);
            seat.id = seatId;
            seats.add(seat);
        }

        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setEventId(testEvent.id);
        dto.setUserId(targetUser.id);
        dto.setSeatIds(seatIds);
        dto.setDeductAllowance(true);

        when(userRepository.findByIdOptional(targetUser.id)).thenReturn(Optional.of(targetUser));
        when(eventRepository.findByIdOptional(testEvent.id)).thenReturn(Optional.of(testEvent));
        when(eventAccessService.isManager(eq(testEvent), any())).thenReturn(true);
        when(seatRepository.findByIds(any())).thenReturn(seats);
        when(eventUserAllowanceRepository.findByUserAndEvent(targetUser, testEvent))
                .thenReturn(Optional.of(allowance));

        reservationService.createReservations(dto, managerUser);

        // Allowance decremented from 10 down to 5 (10 - 5 seats)
        assertEquals(5, allowance.getReservationsAllowedCount());

        // Persist called exactly ONCE after seat loop, rather than 5 times in loop
        verify(eventUserAllowanceRepository, times(1)).persist(allowance);
    }
}
