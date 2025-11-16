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
package de.felixhertweck.seatreservation.reservation.resource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.reservation.dto.UserEventLocationResponseDTO;
import de.felixhertweck.seatreservation.reservation.service.EventLocationService;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/user/locations")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({Roles.USER})
public class EventLocationResource {
    private static final Logger LOG = Logger.getLogger(EventLocationResource.class);

    @Inject EventLocationService eventLocationService;

    @Inject SecurityIdentity securityIdentity;

    @GET
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = UserEventLocationResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: User not found")
    public List<UserEventLocationResponseDTO> getLocations() {
        String username = securityIdentity.getPrincipal().getName();
        LOG.debugf("Received GET request to /api/user/locations for user: %s", username);
        List<UserEventLocationResponseDTO> locations =
                eventLocationService.getLocationsForCurrentUser(username);
        LOG.debugf("Returning %d locations for user: %s", locations.size(), username);
        return locations;
    }
}
