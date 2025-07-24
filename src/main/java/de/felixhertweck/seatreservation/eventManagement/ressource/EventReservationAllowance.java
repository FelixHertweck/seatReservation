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
package de.felixhertweck.seatreservation.eventManagement.ressource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/manager/reservationAllowance")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventReservationAllowance {

    @Inject EventService eventService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(responseCode = "200", description = "OK")
    public EventUserAllowancesDto setReservationsAllowedForUser(
            @Valid EventUserAllowancesDto userReservationAllowanceDTO) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.setReservationsAllowedForUser(userReservationAllowanceDTO, currentUser);
    }

    @GET
    @Path("/{id}")
    public EventUserAllowancesDto getReservationAllowanceById(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.getReservationAllowanceById(id, currentUser);
    }

    @GET
    public List<EventUserAllowancesDto> getReservationAllowances() {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.getReservationAllowances(currentUser);
    }

    @GET
    @Path("/event/{eventId}")
    public List<EventUserAllowancesDto> getReservationAllowancesByEventId(
            @PathParam("eventId") Long eventId) {
        User currentUser = userSecurityContext.getCurrentUser();
        return eventService.getReservationAllowancesByEventId(eventId, currentUser);
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "No Content")
    public void deleteReservationAllowance(@PathParam("id") Long id) {
        User currentUser = userSecurityContext.getCurrentUser();
        eventService.deleteReservationAllowance(id, currentUser);
    }
}
