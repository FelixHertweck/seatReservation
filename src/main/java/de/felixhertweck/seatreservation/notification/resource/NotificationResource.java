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
package de.felixhertweck.seatreservation.notification.resource;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.notification.dto.NotificationPageDTO;
import de.felixhertweck.seatreservation.notification.dto.UnreadCountDTO;
import de.felixhertweck.seatreservation.notification.enums.NotificationCategory;
import de.felixhertweck.seatreservation.notification.service.PushNotificationService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/** REST Resource for in-app user notifications. */
@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResource.class);

    @Inject PushNotificationService notificationService;
    @Inject UserSecurityContext userSecurityContext;

    /** Gets paginated notifications for current user. */
    @GET
    @APIResponse(
            responseCode = "200",
            description = "Paginated list of user notifications retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public NotificationPageDTO getNotifications(
            @QueryParam("unreadOnly") Boolean unreadOnly,
            @QueryParam("category") NotificationCategory category,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "GET /api/notifications requested for user ID: %s, unreadOnly: %s, category: %s",
                (Object) currentUser.id, unreadOnly, category);
        return notificationService.getUserNotifications(
                currentUser, unreadOnly, category, page, size);
    }

    /** Gets total unread notification count for current user. */
    @GET
    @Path("/unread-count")
    @APIResponse(
            responseCode = "200",
            description = "Unread notification count retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public UnreadCountDTO getUnreadCount() {
        User currentUser = userSecurityContext.getCurrentUser();
        long unreadCount = notificationService.getUnreadCount(currentUser);
        return new UnreadCountDTO(unreadCount);
    }

    /** Marks a single notification as read. */
    @PATCH
    @Path("/{id}/read")
    @APIResponse(responseCode = "204", description = "Notification marked as read successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "Notification not found")
    public Response markAsRead(@PathParam("id") UUID id) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "PATCH /api/notifications/%s/read requested for user ID: %s",
                id, (Object) currentUser.id);
        notificationService.markAsRead(id, currentUser);
        return Response.noContent().build();
    }

    /** Marks all notifications as read for current user. */
    @PATCH
    @Path("/read-all")
    @APIResponse(
            responseCode = "200",
            description = "All notifications marked as read successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response markAllAsRead() {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "PATCH /api/notifications/read-all requested for user ID: %s",
                (Object) currentUser.id);
        notificationService.markAllAsRead(currentUser);
        return Response.ok(new UnreadCountDTO(0)).build();
    }

    /** Deletes a notification by ID. */
    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Notification deleted successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "404", description = "Notification not found")
    public Response deleteNotification(@PathParam("id") UUID id) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf(
                "DELETE /api/notifications/%s requested for user ID: %s",
                id, (Object) currentUser.id);
        notificationService.deleteNotification(id, currentUser);
        return Response.noContent().build();
    }
}
