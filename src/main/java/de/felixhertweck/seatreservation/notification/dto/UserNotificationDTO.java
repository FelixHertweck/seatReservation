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
package de.felixhertweck.seatreservation.notification.dto;

import java.time.Instant;
import java.util.UUID;

import de.felixhertweck.seatreservation.model.entity.UserNotification;
import de.felixhertweck.seatreservation.notification.enums.ActionType;
import de.felixhertweck.seatreservation.notification.enums.NotificationCategory;
import de.felixhertweck.seatreservation.notification.enums.NotificationPriority;

public record UserNotificationDTO(
        UUID id,
        NotificationCategory category,
        String title,
        String message,
        NotificationPriority priority,
        boolean isRead,
        ActionType actionType,
        String actionUrl,
        String actionLabel,
        String metadata,
        Instant createdAt) {

    public static UserNotificationDTO fromEntity(UserNotification entity) {
        return new UserNotificationDTO(
                entity.getId(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getPriority(),
                entity.isRead(),
                entity.getActionType(),
                entity.getActionUrl(),
                entity.getActionLabel(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }
}
