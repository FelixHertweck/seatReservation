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
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesCreateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.eventManagement.service.EventReservationAllowanceService;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/manager/reservationAllowance")
@RolesAllowed({Roles.MANAGER, Roles.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventReservationAllowanceResource {

    private static final Logger LOG = Logger.getLogger(EventReservationAllowanceResource.class);

    @Inject EventReservationAllowanceService eventReservationAllowanceService;

    @Inject UserSecurityContext userSecurityContext;

    @POST
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = EventUserAllowancesDto.class)))
    public Set<EventUserAllowancesDto> setReservationsAllowedForUser(
            @Valid EventUserAllowancesCreateDto userReservationAllowanceDTO) {
        LOG.infof(
                "Received POST request to /api/manager/reservationAllowance to set reservation"
                        + " allowance.");
        LOG.debugf("EventUserAllowancesDto received: %s", userReservationAllowanceDTO.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        Set<EventUserAllowancesDto> result =
                eventReservationAllowanceService.setReservationsAllowedForUser(
                        userReservationAllowanceDTO, currentUser);
        LOG.infof(
                "Reservation allowance set successfully for user IDs %s and event ID %d.",
                userReservationAllowanceDTO.getUserIds(), userReservationAllowanceDTO.getEventId());
        return result;
    }

    @PUT
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventUserAllowancesDto.class)))
    public EventUserAllowancesDto updateReservationAllowance(
            @Valid EventUserAllowanceUpdateDto eventUserAllowanceUpdateDto) {
        LOG.infof(
                "Received PUT request to /api/manager/reservationAllowance to update reservation"
                        + " allowance with ID %d.",
                eventUserAllowanceUpdateDto.id());
        LOG.debugf(
                "EventUserAllowanceUpdateDto received: %s", eventUserAllowanceUpdateDto.toString());
        User currentUser = userSecurityContext.getCurrentUser();
        EventUserAllowancesDto result =
                eventReservationAllowanceService.updateReservationAllowance(
                        eventUserAllowanceUpdateDto, currentUser);
        LOG.infof(
                "Reservation allowance with ID %d updated successfully.",
                eventUserAllowanceUpdateDto.id());
        return result;
    }

    @GET
    @Path("/{id}")
    public EventUserAllowancesDto getReservationAllowanceById(@PathParam("id") Long id) {
        LOG.infof("Received GET request to /api/manager/reservationAllowance/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        EventUserAllowancesDto result =
                eventReservationAllowanceService.getReservationAllowanceById(id, currentUser);
        if (result != null) {
            LOG.infof("Successfully retrieved reservation allowance with ID %d.", id);
        } else {
            LOG.warnf("Reservation allowance with ID %d not found.", id);
        }
        return result;
    }

    @GET
    public List<EventUserAllowancesDto> getReservationAllowances() {
        LOG.infof(
                "Received GET request to /api/manager/reservationAllowance to get all allowances.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventUserAllowancesDto> result =
                eventReservationAllowanceService.getReservationAllowances(currentUser);
        LOG.infof("Successfully retrieved %d reservation allowances.", result.size());
        return result;
    }

    @GET
    @Path("/event/{eventId}")
    public List<EventUserAllowancesDto> getReservationAllowancesByEventId(
            @PathParam("eventId") Long eventId) {
        LOG.infof("Received GET request to /api/manager/reservationAllowance/event/%d.", eventId);
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventUserAllowancesDto> result =
                eventReservationAllowanceService.getReservationAllowancesByEventId(
                        eventId, currentUser);
        LOG.infof(
                "Successfully retrieved %d reservation allowances for event ID %d.",
                result.size(), eventId);
        return result;
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "No Content")
    public void deleteReservationAllowance(@PathParam("id") Long id) {
        LOG.infof(
                "Received DELETE request to /api/manager/reservationAllowance/%d to delete"
                        + " allowance.",
                id);
        User currentUser = userSecurityContext.getCurrentUser();
        eventReservationAllowanceService.deleteReservationAllowance(id, currentUser);
        LOG.infof("Reservation allowance with ID %d deleted successfully.", id);
    }
}
