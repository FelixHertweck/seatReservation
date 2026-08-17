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

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.notification.dto.PushSubscriptionRequestDTO;
import de.felixhertweck.seatreservation.notification.dto.VapidPublicKeyDTO;
import de.felixhertweck.seatreservation.notification.service.PushNotificationService;
import de.felixhertweck.seatreservation.notification.service.WebPushService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/** REST resource for managing browser Web Push subscriptions. */
@Path("/api/push/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PushSubscriptionResource {

    private static final Logger LOG = Logger.getLogger(PushSubscriptionResource.class);

    @Inject PushNotificationService notificationService;
    @Inject WebPushService webPushService;
    @Inject UserSecurityContext userSecurityContext;

    /** Returns the server's VAPID public key for Web Push subscription. */
    @GET
    @Path("/vapid-public-key")
    @PermitAll
    @APIResponse(responseCode = "200", description = "Returns VAPID Public Key")
    public Response getVapidPublicKey() {
        return Response.ok(new VapidPublicKeyDTO(webPushService.getPublicKey())).build();
    }

    /** Registers or updates a Web Push subscription. */
    @POST
    @APIResponse(responseCode = "201", description = "Push subscription registered successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid subscription data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response registerSubscription(@Valid PushSubscriptionRequestDTO request) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf("POST /api/push/subscriptions for user ID: %s", (Object) currentUser.id);
        notificationService.registerPushSubscription(currentUser, request);
        return Response.status(Response.Status.CREATED).build();
    }

    /** Unregisters a Web Push subscription by endpoint. */
    @DELETE
    @APIResponse(responseCode = "204", description = "Push subscription unregistered successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response unregisterSubscription(@QueryParam("endpoint") String endpoint) {
        User currentUser = userSecurityContext.getCurrentUser();
        LOG.debugf("DELETE /api/push/subscriptions for user ID: %s", (Object) currentUser.id);
        if (endpoint != null && !endpoint.isBlank()) {
            notificationService.unregisterPushSubscription(currentUser, endpoint);
        }
        return Response.noContent().build();
    }
}
