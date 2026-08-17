/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
package de.felixhertweck.seatreservation.notification.service;

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.common.events.EventCancelledEvent;
import de.felixhertweck.seatreservation.common.events.ReservationCancelledEvent;
import de.felixhertweck.seatreservation.common.events.ReservationCreatedEvent;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.UserNotification;
import de.felixhertweck.seatreservation.model.entity.UserPushSubscription;
import de.felixhertweck.seatreservation.model.repository.UserNotificationRepository;
import de.felixhertweck.seatreservation.model.repository.UserPushSubscriptionRepository;
import de.felixhertweck.seatreservation.notification.dto.NotificationPageDTO;
import de.felixhertweck.seatreservation.notification.dto.PushSubscriptionRequestDTO;
import de.felixhertweck.seatreservation.notification.dto.UserNotificationDTO;
import de.felixhertweck.seatreservation.notification.enums.ActionType;
import de.felixhertweck.seatreservation.notification.enums.NotificationCategory;
import de.felixhertweck.seatreservation.notification.enums.NotificationPriority;
import de.felixhertweck.seatreservation.notification.exception.NotificationNotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class PushNotificationServiceTest {

    @InjectMock UserNotificationRepository notificationRepository;

    @InjectMock UserPushSubscriptionRepository pushSubscriptionRepository;

    @Inject PushNotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Mockito.reset(notificationRepository, pushSubscriptionRepository);
        testUser =
                new User(
                        "john_doe",
                        "john@example.com",
                        true,
                        false,
                        "hash",
                        "salt",
                        "John",
                        "Doe",
                        Set.of("USER"),
                        Set.of());
        testUser.id = id(100);
    }

    @Test
    void createNotification_Success() {
        UserNotificationDTO result =
                notificationService.createNotification(
                        testUser,
                        NotificationCategory.BOOKING,
                        "Test Title",
                        "Test Message",
                        NotificationPriority.NORMAL,
                        ActionType.NAVIGATE,
                        "/test-url",
                        "Click Here",
                        null);

        assertNotNull(result);
        assertEquals(NotificationCategory.BOOKING, result.category());
        assertEquals("Test Title", result.title());
        assertEquals("Test Message", result.message());
        assertEquals(NotificationPriority.NORMAL, result.priority());
        assertEquals(ActionType.NAVIGATE, result.actionType());
        assertEquals("/test-url", result.actionUrl());
        assertEquals("Click Here", result.actionLabel());
        assertFalse(result.isRead());
        verify(notificationRepository, times(1)).persist(any(UserNotification.class));
    }

    @Test
    void getUserNotifications_Success() {
        UserNotification entity =
                new UserNotification(
                        testUser,
                        NotificationCategory.BOOKING,
                        "Title",
                        "Msg",
                        NotificationPriority.NORMAL,
                        ActionType.NAVIGATE,
                        "/url",
                        "Label",
                        null);
        entity.id = id(1);

        when(notificationRepository.findByUser(
                        testUser, false, NotificationCategory.BOOKING, 0, 10))
                .thenReturn(List.of(entity));
        when(notificationRepository.countByUser(testUser, false, NotificationCategory.BOOKING))
                .thenReturn(1L);
        when(notificationRepository.countUnreadByUser(testUser)).thenReturn(1L);

        NotificationPageDTO page =
                notificationService.getUserNotifications(
                        testUser, false, NotificationCategory.BOOKING, 0, 10);

        assertNotNull(page);
        assertEquals(1, page.items().size());
        assertEquals(1L, page.totalItems());
        assertEquals(1L, page.unreadCount());
        assertEquals("Title", page.items().get(0).title());
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countUnreadByUser(testUser)).thenReturn(5L);

        long count = notificationService.getUnreadCount(testUser);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_Success() {
        UUID notifId = id(1);
        when(notificationRepository.markAsRead(notifId, testUser)).thenReturn(true);

        notificationService.markAsRead(notifId, testUser);

        verify(notificationRepository, times(1)).markAsRead(notifId, testUser);
    }

    @Test
    void markAsRead_NotFound() {
        UUID notifId = id(1);
        when(notificationRepository.markAsRead(notifId, testUser)).thenReturn(false);

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.markAsRead(notifId, testUser));
    }

    @Test
    void markAllAsRead_Success() {
        when(notificationRepository.markAllAsReadByUser(testUser)).thenReturn(3L);

        long updated = notificationService.markAllAsRead(testUser);

        assertEquals(3L, updated);
        verify(notificationRepository, times(1)).markAllAsReadByUser(testUser);
    }

    @Test
    void deleteNotification_Success() {
        UUID notifId = id(1);
        when(notificationRepository.deleteByIdAndUser(notifId, testUser)).thenReturn(true);

        notificationService.deleteNotification(notifId, testUser);

        verify(notificationRepository, times(1)).deleteByIdAndUser(notifId, testUser);
    }

    @Test
    void deleteNotification_NotFound() {
        UUID notifId = id(1);
        when(notificationRepository.deleteByIdAndUser(notifId, testUser)).thenReturn(false);

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.deleteNotification(notifId, testUser));
    }

    @Test
    void registerPushSubscription_NewSubscription() {
        PushSubscriptionRequestDTO request =
                new PushSubscriptionRequestDTO("https://push.endpoint/123", "p256key", "authkey");
        when(pushSubscriptionRepository.findByEndpoint(request.endpoint()))
                .thenReturn(Optional.empty());

        notificationService.registerPushSubscription(testUser, request);

        verify(pushSubscriptionRepository, times(1)).persist(any(UserPushSubscription.class));
    }

    @Test
    void registerPushSubscription_ExistingSubscription() {
        PushSubscriptionRequestDTO request =
                new PushSubscriptionRequestDTO("https://push.endpoint/123", "new_p256", "new_auth");
        UserPushSubscription existing =
                new UserPushSubscription(testUser, request.endpoint(), "old_p256", "old_auth");
        when(pushSubscriptionRepository.findByEndpoint(request.endpoint()))
                .thenReturn(Optional.of(existing));

        notificationService.registerPushSubscription(testUser, request);

        assertEquals("new_p256", existing.getP256dh());
        assertEquals("new_auth", existing.getAuth());
        verify(pushSubscriptionRepository, never()).persist(any(UserPushSubscription.class));
    }

    @Test
    void unregisterPushSubscription_Success() {
        String endpoint = "https://push.endpoint/123";

        notificationService.unregisterPushSubscription(testUser, endpoint);

        verify(pushSubscriptionRepository, times(1)).deleteByEndpointAndUser(endpoint, testUser);
    }

    @Test
    void onReservationCreated_FiresNotification() {
        Event event = new Event();
        event.setName("Concert Event");

        Reservation reservation = new Reservation();
        reservation.setEvent(event);
        reservation.setUser(testUser);

        ReservationCreatedEvent eventObj =
                new ReservationCreatedEvent(testUser, List.of(reservation));

        notificationService.onReservationCreated(eventObj);

        verify(notificationRepository, times(1)).persist(any(UserNotification.class));
    }

    @Test
    void onReservationCancelled_FiresNotification() {
        Reservation reservation = new Reservation();
        reservation.setUser(testUser);

        ReservationCancelledEvent eventObj =
                new ReservationCancelledEvent(
                        testUser, List.of(reservation), List.of(), "Cancellation notice");

        notificationService.onReservationCancelled(eventObj);

        verify(notificationRepository, times(1)).persist(any(UserNotification.class));
    }

    @Test
    void onEventCancelled_FiresNotificationForAffectedUsers() {
        Reservation reservation = new Reservation();
        reservation.setUser(testUser);

        EventCancelledEvent eventObj =
                new EventCancelledEvent(
                        id(50),
                        "Festival 2026",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        "Hall A",
                        "Weather issues",
                        List.of(reservation));

        notificationService.onEventCancelled(eventObj);

        verify(notificationRepository, times(1)).persist(any(UserNotification.class));
    }
}
