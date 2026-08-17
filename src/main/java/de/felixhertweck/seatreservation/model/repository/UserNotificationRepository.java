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
package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.UserNotification;
import de.felixhertweck.seatreservation.notification.enums.NotificationCategory;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserNotificationRepository implements PanacheRepositoryBase<UserNotification, UUID> {

    private static final Logger LOG = Logger.getLogger(UserNotificationRepository.class);

    /** Finds notifications for a specific user with optional unread filter and category filter. */
    public List<UserNotification> findByUser(
            User user,
            Boolean unreadOnly,
            NotificationCategory category,
            int pageIndex,
            int pageSize) {
        LOG.debugf(
                "Finding notifications for user ID: %s, unreadOnly: %s, category: %s, page: %d",
                (Object) user.id, unreadOnly, category, pageIndex);

        StringBuilder queryStr = new StringBuilder("user = ?1");
        if (Boolean.TRUE.equals(unreadOnly)) {
            queryStr.append(" and isRead = false");
        }
        if (category != null) {
            queryStr.append(" and category = ?2");
        }
        queryStr.append(" order by createdAt desc");

        PanacheQuery<UserNotification> query =
                category != null
                        ? find(queryStr.toString(), user, category)
                        : find(queryStr.toString(), user);
        query.page(Page.of(pageIndex, pageSize));
        return query.list();
    }

    /** Counts unread notifications for a user. */
    public long countUnreadByUser(User user) {
        return count("user = ?1 and isRead = false", user);
    }

    /** Counts total notifications matching criteria for a user. */
    public long countByUser(User user, Boolean unreadOnly, NotificationCategory category) {
        StringBuilder queryStr = new StringBuilder("user = ?1");
        if (Boolean.TRUE.equals(unreadOnly)) {
            queryStr.append(" and isRead = false");
        }
        if (category != null) {
            queryStr.append(" and category = ?2");
            return count(queryStr.toString(), user, category);
        }
        return count(queryStr.toString(), user);
    }

    /** Finds a notification by ID and User for ownership check. */
    public Optional<UserNotification> findByIdAndUser(UUID id, User user) {
        return find("id = ?1 and user = ?2", id, user).firstResultOptional();
    }

    /** Marks a specific notification as read. */
    @Transactional
    public boolean markAsRead(UUID id, User user) {
        Optional<UserNotification> notif = findByIdAndUser(id, user);
        if (notif.isPresent()) {
            UserNotification notification = notif.get();
            notification.setRead(true);
            return true;
        }
        return false;
    }

    /** Marks all notifications as read for a user. */
    @Transactional
    public long markAllAsReadByUser(User user) {
        LOG.debugf("Marking all notifications as read for user ID: %s", (Object) user.id);
        return update("isRead = true where user = ?1 and isRead = false", user);
    }

    /** Deletes a notification by ID and User. */
    @Transactional
    public boolean deleteByIdAndUser(UUID id, User user) {
        return delete("id = ?1 and user = ?2", id, user) > 0;
    }
}
