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
package de.felixhertweck.seatreservation.management.resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.management.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.management.dto.EventUserAllowancesCreateDto;
import de.felixhertweck.seatreservation.management.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.management.service.EventReservationAllowanceService;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
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
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event or user not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Allowance already exists for this user and event")
    public Set<EventUserAllowancesDto> setReservationsAllowedForUser(
            @Valid EventUserAllowancesCreateDto userReservationAllowanceDTO) {
        LOG.debugf(
                "Received POST request to /api/manager/reservationAllowance to set reservation"
                        + " allowance.");
        User currentUser = userSecurityContext.getCurrentUser();
        Set<EventUserAllowancesDto> result =
                eventReservationAllowanceService.setReservationsAllowedForUser(
                        userReservationAllowanceDTO, currentUser);
        LOG.debugf(
                "Reservation allowance set successfully for user IDs %s and event ID %d.",
                userReservationAllowanceDTO.getUserIds(), userReservationAllowanceDTO.getEventId());
        return result;
    }

    @PUT
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = EventUserAllowancesDto.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Allowance with specified ID not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: Allowance already exists for this user and event")
    public EventUserAllowancesDto updateReservationAllowance(
            @Valid EventUserAllowanceUpdateDto eventUserAllowanceUpdateDto) {
        LOG.debugf(
                "Received PUT request to /api/manager/reservationAllowance to update reservation"
                        + " allowance with ID %d.",
                eventUserAllowanceUpdateDto.id());
        User currentUser = userSecurityContext.getCurrentUser();
        EventUserAllowancesDto result =
                eventReservationAllowanceService.updateReservationAllowance(
                        eventUserAllowanceUpdateDto, currentUser);
        LOG.debugf(
                "Reservation allowance with ID %d updated successfully.",
                eventUserAllowanceUpdateDto.id());
        return result;
    }

    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "Reservation allowance retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Allowance with specified ID not found")
    public EventUserAllowancesDto getReservationAllowanceById(@PathParam("id") Long id) {
        LOG.debugf("Received GET request to /api/manager/reservationAllowance/%d.", id);
        User currentUser = userSecurityContext.getCurrentUser();
        EventUserAllowancesDto result =
                eventReservationAllowanceService.getReservationAllowanceById(id, currentUser);
        if (result != null) {
            LOG.debugf("Successfully retrieved reservation allowance with ID %d.", id);
        } else {
            LOG.warnf("Reservation allowance with ID %d not found.", id);
        }
        return result;
    }

    @GET
    @APIResponse(
            responseCode = "200",
            description = "List of reservation allowances retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    public List<EventUserAllowancesDto> getReservationAllowances() {
        LOG.debugf(
                "Received GET request to /api/manager/reservationAllowance to get all allowances.");
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventUserAllowancesDto> result =
                eventReservationAllowanceService.getReservationAllowances(currentUser);
        LOG.debugf("Successfully retrieved %d reservation allowances.", result.size());
        return result;
    }

    @GET
    @Path("/event/{eventId}")
    @APIResponse(
            responseCode = "200",
            description =
                    "List of reservation allowances for a specific event retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: Event with specified ID not found")
    public List<EventUserAllowancesDto> getReservationAllowancesByEventId(
            @PathParam("eventId") Long eventId) {
        LOG.debugf("Received GET request to /api/manager/reservationAllowance/event/%d.", eventId);
        User currentUser = userSecurityContext.getCurrentUser();
        List<EventUserAllowancesDto> result =
                eventReservationAllowanceService.getReservationAllowancesByEventId(
                        eventId, currentUser);
        LOG.debugf(
                "Successfully retrieved %d reservation allowances for event ID %d.",
                result.size(), eventId);
        return result;
    }

    @DELETE
    @APIResponse(responseCode = "204", description = "No Content")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid input")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only MANAGER or ADMIN roles can access this resource")
    @APIResponse(
            responseCode = "404",
            description = "Not Found: Allowance with specified ID not found")
    public void deleteReservationAllowance(@QueryParam("ids") List<Long> ids) {
        LOG.debugf(
                "Received DELETE request to /api/manager/reservationAllowance with IDs: %s",
                ids != null ? ids : Collections.emptyList());
        User currentUser = userSecurityContext.getCurrentUser();
        eventReservationAllowanceService.deleteReservationAllowance(ids, currentUser);
        LOG.debugf(
                "Reservation allowance with IDs %s deleted successfully.",
                ids != null ? ids : Collections.emptyList());
    }
}
