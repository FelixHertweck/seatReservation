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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
import org.jboss.logging.Logger;

/**
 * Manages push/in-app notifications: persistence, delivery, and read/unread state. Named {@code
 * PushNotificationService} (rather than plain {@code NotificationService}) to avoid colliding with
 * {@link de.felixhertweck.seatreservation.email.service.NotificationService}, which handles the
 * unrelated concern of scheduling and sending reminder/cancellation emails.
 */
@ApplicationScoped
public class PushNotificationService {

    private static final Logger LOG = Logger.getLogger(PushNotificationService.class);

    @Inject UserNotificationRepository notificationRepository;
    @Inject UserPushSubscriptionRepository pushSubscriptionRepository;
    @Inject WebPushService webPushService;

    /**
     * Self-injected reference used to invoke {@link #persistNotification} through the CDI proxy
     * rather than by self-invocation, so its own transactional boundary (REQUIRES_NEW) actually
     * applies instead of silently joining whatever transaction the caller happens to be in.
     */
    @Inject PushNotificationService self;

    /**
     * Persists an in-app notification in its own, independent transaction. Kept separate from push
     * delivery (which does blocking network I/O) so a slow or failing push send never holds this
     * transaction - or a caller's ambient transaction - open, and a push-related failure can never
     * mark an unrelated caller transaction rollback-only.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public UserNotification persistNotification(
            User user,
            NotificationCategory category,
            String title,
            String message,
            NotificationPriority priority,
            ActionType actionType,
            String actionUrl,
            String actionLabel,
            String metadata) {
        LOG.debugf(
                "Creating notification for user ID: %s, category: %s, title: %s",
                (Object) user.id, category, title);

        UserNotification notification =
                new UserNotification(
                        user,
                        category,
                        title,
                        message,
                        priority,
                        actionType,
                        actionUrl,
                        actionLabel,
                        metadata);

        notificationRepository.persist(notification);
        return notification;
    }

    /**
     * Creates and persists an in-app notification for a user, then pushes it to their devices.
     * Persisting runs and commits in its own short transaction before push delivery starts, so the
     * blocking push HTTP calls never run with a database transaction/connection held open.
     */
    public UserNotificationDTO createNotification(
            User user,
            NotificationCategory category,
            String title,
            String message,
            NotificationPriority priority,
            ActionType actionType,
            String actionUrl,
            String actionLabel,
            String metadata) {
        UserNotification notification =
                self.persistNotification(
                        user,
                        category,
                        title,
                        message,
                        priority,
                        actionType,
                        actionUrl,
                        actionLabel,
                        metadata);
        sendPushToUserDevices(user, title, message, actionUrl);
        return UserNotificationDTO.fromEntity(notification);
    }

    /** Pushes a notification to every browser the user has subscribed, pruning dead endpoints. */
    private void sendPushToUserDevices(User user, String title, String message, String actionUrl) {
        for (UserPushSubscription subscription : pushSubscriptionRepository.findByUser(user)) {
            int status =
                    webPushService.sendPushNotification(subscription, title, message, actionUrl);
            // 404/410: the push service confirms the subscription no longer exists - stop trying.
            if (status == 404 || status == 410) {
                pushSubscriptionRepository.deleteByEndpointAndUser(
                        subscription.getEndpoint(), user);
            }
        }
    }

    /** Retrieves paginated notifications for a user with optional filtering. */
    public NotificationPageDTO getUserNotifications(
            User user,
            Boolean unreadOnly,
            NotificationCategory category,
            int pageIndex,
            int pageSize) {
        List<UserNotification> entities =
                notificationRepository.findByUser(user, unreadOnly, category, pageIndex, pageSize);
        List<UserNotificationDTO> dtos =
                entities.stream().map(UserNotificationDTO::fromEntity).toList();

        long totalItems = notificationRepository.countByUser(user, unreadOnly, category);
        long unreadCount = notificationRepository.countUnreadByUser(user);

        return new NotificationPageDTO(dtos, totalItems, pageIndex, pageSize, unreadCount);
    }

    /** Returns unread notification count for a user. */
    public long getUnreadCount(User user) {
        return notificationRepository.countUnreadByUser(user);
    }

    /** Marks a single notification as read for a user. */
    @Transactional
    public void markAsRead(UUID id, User user) {
        boolean updated = notificationRepository.markAsRead(id, user);
        if (!updated) {
            throw new NotificationNotFoundException("Notification not found for user");
        }
    }

    /** Marks all notifications as read for a user. */
    @Transactional
    public long markAllAsRead(User user) {
        return notificationRepository.markAllAsReadByUser(user);
    }

    /** Deletes a notification by ID for a user. */
    @Transactional
    public void deleteNotification(UUID id, User user) {
        boolean deleted = notificationRepository.deleteByIdAndUser(id, user);
        if (!deleted) {
            throw new NotificationNotFoundException("Notification not found for user");
        }
    }

    /** Saves or updates a browser push subscription for a user. */
    @Transactional
    public void registerPushSubscription(User user, PushSubscriptionRequestDTO request) {
        LOG.debugf("Registering push subscription for user ID: %s", (Object) user.id);
        Optional<UserPushSubscription> existing =
                pushSubscriptionRepository.findByEndpoint(request.endpoint());

        if (existing.isPresent()) {
            UserPushSubscription sub = existing.get();
            if (!sub.getUser().id.equals(user.id)) {
                // Endpoints are per-browser, not per-account: re-subscribing on a device
                // previously used by another user (shared/kiosk device, or a stale subscription
                // left behind by a non-graceful logout) legitimately hands the subscription over.
                // Logged at WARN, without the endpoint value, so a burst of unexpected handoffs is
                // detectable without leaking endpoint URLs (a would-be hijacker's target) into
                // logs.
                LOG.warnf(
                        "Push subscription re-registered under user ID: %s; it previously belonged"
                                + " to user ID: %s",
                        (Object) user.id, (Object) sub.getUser().id);
            }
            sub.setUser(user);
            sub.setP256dh(request.p256dh());
            sub.setAuth(request.auth());
        } else {
            UserPushSubscription sub =
                    new UserPushSubscription(
                            user, request.endpoint(), request.p256dh(), request.auth());
            pushSubscriptionRepository.persist(sub);
        }
    }

    /** Unregisters a browser push subscription by endpoint. */
    @Transactional
    public void unregisterPushSubscription(User user, String endpoint) {
        LOG.debugf("Unregistering push subscription for user ID: %s", (Object) user.id);
        pushSubscriptionRepository.deleteByEndpointAndUser(endpoint, user);
    }

    /**
     * Reacts to reservation creation by generating an in-app notification. Fires only after the
     * reservation-creation transaction has committed successfully (TransactionPhase.AFTER_SUCCESS),
     * so a failure anywhere in notification handling can never roll back a reservation the user
     * already successfully booked.
     */
    public void onReservationCreated(
            @jakarta.enterprise.event.Observes(during = TransactionPhase.AFTER_SUCCESS)
                    de.felixhertweck.seatreservation.common.events.ReservationCreatedEvent event) {
        if (event.user() == null
                || event.reservations() == null
                || event.reservations().isEmpty()) {
            return;
        }
        try {
            String eventName = event.reservations().get(0).getEvent().getName();
            UUID eventId = event.reservations().get(0).getEvent().getId();
            int count = event.reservations().size();
            createNotification(
                    event.user(),
                    NotificationCategory.BOOKING,
                    NotificationMessages.BOOKING_CONFIRMED_TITLE,
                    NotificationMessages.bookingConfirmedMessage(count, eventName),
                    NotificationPriority.NORMAL,
                    ActionType.NAVIGATE,
                    "/events/reservations?eventId=" + eventId,
                    NotificationMessages.VIEW_BOOKING_ACTION_LABEL,
                    null);
        } catch (Exception e) {
            LOG.error("Failed to create in-app notification for reservation creation", e);
        }
    }

    /**
     * Reacts to reservation cancellation by generating an in-app notification. Fires only after the
     * cancellation transaction has committed successfully, for the same reason as {@link
     * #onReservationCreated}.
     */
    public void onReservationCancelled(
            @jakarta.enterprise.event.Observes(during = TransactionPhase.AFTER_SUCCESS)
                    de.felixhertweck.seatreservation.common.events.ReservationCancelledEvent
                            event) {
        if (event.user() == null) {
            return;
        }
        try {
            int count =
                    event.deletedReservations() != null ? event.deletedReservations().size() : 0;
            String message =
                    event.noticeMessage() != null
                            ? event.noticeMessage()
                            : NotificationMessages.reservationCancelledMessage(count);
            UUID eventId = firstEventId(event.deletedReservations(), event.activeReservations());
            String actionUrl =
                    eventId != null
                            ? "/events/reservations?eventId=" + eventId
                            : "/events/reservations";
            createNotification(
                    event.user(),
                    NotificationCategory.BOOKING,
                    NotificationMessages.RESERVATION_CANCELLED_TITLE,
                    message,
                    NotificationPriority.HIGH,
                    ActionType.NAVIGATE,
                    actionUrl,
                    NotificationMessages.VIEW_RESERVATIONS_ACTION_LABEL,
                    null);
        } catch (Exception e) {
            LOG.error("Failed to create in-app notification for reservation cancellation", e);
        }
    }

    /**
     * Returns the event ID of the first reservation with a resolvable event across the given lists,
     * if any.
     */
    @SafeVarargs
    private static UUID firstEventId(List<Reservation>... reservationLists) {
        for (List<Reservation> reservations : reservationLists) {
            if (reservations == null) {
                continue;
            }
            for (Reservation reservation : reservations) {
                de.felixhertweck.seatreservation.model.entity.Event event = reservation.getEvent();
                if (event != null) {
                    return event.getId();
                }
            }
        }
        return null;
    }

    /**
     * Reacts to event cancellation by generating in-app notifications for affected users. Fires
     * only after the cancellation transaction has committed successfully, for the same reason as
     * {@link #onReservationCreated}.
     */
    public void onEventCancelled(
            @jakarta.enterprise.event.Observes(during = TransactionPhase.AFTER_SUCCESS)
                    de.felixhertweck.seatreservation.common.events.EventCancelledEvent event) {
        if (event.cancelledReservations() == null) {
            return;
        }
        event.cancelledReservations().stream()
                .map(de.felixhertweck.seatreservation.model.entity.Reservation::getUser)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(
                        user -> {
                            try {
                                String msg =
                                        NotificationMessages.eventCancelledMessage(
                                                event.eventName(), event.cancellationReason());
                                createNotification(
                                        user,
                                        NotificationCategory.EVENT_REMINDER,
                                        NotificationMessages.EVENT_CANCELLED_TITLE,
                                        msg,
                                        NotificationPriority.URGENT,
                                        ActionType.NAVIGATE,
                                        "/events",
                                        NotificationMessages.BROWSE_EVENTS_ACTION_LABEL,
                                        null);
                            } catch (Exception e) {
                                LOG.errorf(
                                        e,
                                        "Failed to create in-app notification for event"
                                                + " cancellation, user ID: %s",
                                        (Object) user.id);
                            }
                        });
    }
}
