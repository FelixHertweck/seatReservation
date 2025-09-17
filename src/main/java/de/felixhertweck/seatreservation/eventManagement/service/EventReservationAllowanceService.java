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
package de.felixhertweck.seatreservation.eventManagement.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.EventNotFoundException;
import de.felixhertweck.seatreservation.common.exception.UserNotFoundException;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowanceUpdateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesCreateDto;
import de.felixhertweck.seatreservation.eventManagement.dto.EventUserAllowancesDto;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventUserAllowance;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import de.felixhertweck.seatreservation.model.repository.EventUserAllowanceRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventReservationAllowanceService {
    private static final Logger LOG = Logger.getLogger(EventService.class);

    @Inject EventRepository eventRepository;

    @Inject UserRepository userRepository;

    @Inject EventUserAllowanceRepository eventUserAllowanceRepository;

    /**
     * Sets the reservation allowance for a user for a specific event. Access control: The manger
     * must be the manager of the event or have the ADMIN role to set allowances.
     *
     * @param dto The DTO containing user IDs and the number of reservations allowed.
     * @param manager The user attempting to set the allowances.
     * @throws EventNotFoundException If the event with the specified ID is not found.
     * @throws UserNotFoundException If any of the specified user IDs are not found.
     * @return A set of DTOs representing the updated reservation allowances for the users.
     */
    @Transactional
    public Set<EventUserAllowancesDto> setReservationsAllowedForUser(
            EventUserAllowancesCreateDto dto, User manager)
            throws EventNotFoundException, UserNotFoundException {
        LOG.debugf(
                "Attempting to set reservation allowance for user IDs: %s, event ID: %d by manager:"
                        + " %s (ID: %d)",
                dto.getUserIds().stream().map(Object::toString).collect(Collectors.joining(", ")),
                dto.getEventId(),
                manager.getUsername(),
                manager.getId());
        Event event = getEventById(dto.getEventId());

        if (!event.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to set reservation allowance for event ID"
                            + " %d.",
                    manager.getUsername(), manager.getId(), dto.getEventId());
            throw new SecurityException("User is not the manager of this event");
        }

        Set<EventUserAllowancesDto> resultAllowances = new HashSet<>();

        dto.getUserIds()
                .forEach(
                        userId -> {
                            User user = getUserById(userId);
                            EventUserAllowance allowance =
                                    eventUserAllowanceRepository
                                            .find("user = ?1 and event = ?2", user, event)
                                            .firstResultOptional()
                                            .orElse(
                                                    new EventUserAllowance(
                                                            user,
                                                            event,
                                                            dto.getReservationsAllowedCount()));

                            allowance.setReservationsAllowedCount(
                                    dto.getReservationsAllowedCount());
                            eventUserAllowanceRepository.persist(allowance);

                            resultAllowances.add(new EventUserAllowancesDto((allowance)));
                        });

        LOG.infof(
                "Reservation allowance set to %d for user IDs %s and event ID %d by manager: %s"
                        + " (ID: %d)",
                dto.getReservationsAllowedCount(),
                dto.getUserIds(),
                dto.getEventId(),
                manager.getUsername(),
                manager.getId());

        return resultAllowances;
    }

    /**
     * Updates an existing reservation allowance. Access control: The manager must be the manager of
     * the event associated with the allowance or have the ADMIN role to update it.
     *
     * @param dto The DTO containing the updated reservation allowance information.
     * @param manager The user attempting to update the allowance.
     * @throws EventNotFoundException If the event or allowance with the specified IDs are not
     *     found.
     * @throws SecurityException If the user is not authorized to update this allowance.
     * @return A DTO representing the updated reservation allowance.
     */
    @Transactional
    public EventUserAllowancesDto updateReservationAllowance(
            EventUserAllowanceUpdateDto dto, User manager)
            throws EventNotFoundException, SecurityException {
        LOG.debugf(
                "Attempting to update reservation allowance with ID: %d for user ID: %d, event ID:"
                        + " %d by manager: %s (ID: %d)",
                dto.id(), dto.userId(), dto.eventId(), manager.getUsername(), manager.getId());

        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(dto.id())
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation allowance with ID %d not found for update"
                                                    + " by manager: %s (ID: %d)",
                                            dto.id(), manager.getUsername(), manager.getId());
                                    return new EventNotFoundException("Allowance not found");
                                });

        if (!allowance.getEvent().getManager().equals(manager)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to update reservation allowance with ID"
                            + " %d.",
                    manager.getUsername(), manager.getId(), dto.id());
            throw new SecurityException("User is not authorized to update this allowance");
        }

        allowance.setReservationsAllowedCount(dto.reservationsAllowedCount());
        eventUserAllowanceRepository.persist(allowance);

        LOG.infof(
                "Reservation allowance with ID %d updated successfully to count %d by manager: %s"
                        + " (ID: %d)",
                dto.id(), dto.reservationsAllowedCount(), manager.getUsername(), manager.getId());
        return new EventUserAllowancesDto(allowance);
    }

    /**
     * Retrieves a reservation allowance by its ID. Access control: The manager must be the manager
     * of the event associated with the allowance or have the ADMIN role to view it.
     *
     * @param id The ID of the reservation allowance to retrieve.
     * @param manager The user attempting to retrieve the allowance.
     * @throws EventNotFoundException If the reservation allowance with the specified ID is not
     *     found.
     * @throws SecurityException If the user is not authorized to view this allowance.
     * @return A DTO representing the reservation allowance.
     */
    public EventUserAllowancesDto getReservationAllowanceById(Long id, User manager) {
        LOG.debugf(
                "Attempting to retrieve reservation allowance with ID: %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation allowance with ID %d not found for"
                                                    + " manager: %s (ID: %d)",
                                            id, manager.getUsername(), manager.getId());
                                    return new EventNotFoundException("Allowance not found");
                                });

        if (!allowance.getEvent().getManager().equals(manager)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to view reservation allowance with ID %d.",
                    manager.getUsername(), manager.getId(), id);
            throw new SecurityException("User is not authorized to view this allowance");
        }
        LOG.debugf(
                "Successfully retrieved reservation allowance with ID %d for manager: %s (ID: %d)",
                id, manager.getUsername(), manager.getId());
        return new EventUserAllowancesDto(allowance);
    }

    /**
     * Retrieves all reservation allowances for the currently authenticated user. Access control: If
     * the user is an administrator, all allowances are returned. Otherwise, only allowances for
     * events managed by the current user are returned.
     *
     * @param currentUser The currently authenticated user.
     * @throws SecurityException If the user is not authorized to view the allowances.
     * @return A list of DTOs representing the reservation allowances for the current user.
     */
    public List<EventUserAllowancesDto> getReservationAllowances(User currentUser)
            throws SecurityException {
        LOG.debugf(
                "Attempting to retrieve all reservation allowances for user: %s (ID: %d)",
                currentUser.getUsername(), currentUser.getId());
        List<EventUserAllowance> allowances;
        if (currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.debug("User is ADMIN, listing all reservation allowances.");
            allowances = eventUserAllowanceRepository.listAll();
        } else {
            LOG.debugf(
                    "User is MANAGER, listing reservation allowances for events managed by user ID:"
                            + " %d",
                    currentUser.getId());
            allowances = eventUserAllowanceRepository.find("event.manager", currentUser).list();
        }
        LOG.infof(
                "Retrieved %d reservation allowances for user: %s (ID: %d)",
                allowances.size(), currentUser.getUsername(), currentUser.getId());
        return allowances.stream().map(EventUserAllowancesDto::new).collect(Collectors.toList());
    }

    /**
     * Retrieves all reservation allowances for a specific event. Access control: The user must be
     * the manager of the event or have the ADMIN role to view the allowances.
     *
     * @param eventId The ID of the event for which to retrieve allowances.
     * @param currentUser The currently authenticated user.
     * @throws SecurityException If the user is not authorized to view the allowances for this
     *     event.
     * @throws EventNotFoundException If the event with the specified ID is not found.
     * @return A list of DTOs representing the reservation allowances for the specified event.
     */
    public List<EventUserAllowancesDto> getReservationAllowancesByEventId(
            Long eventId, User currentUser) throws SecurityException, EventNotFoundException {
        LOG.debugf(
                "Attempting to retrieve reservation allowances for event ID: %d by user: %s (ID:"
                        + " %d)",
                eventId, currentUser.getUsername(), currentUser.getId());
        Event event = getEventById(eventId);
        if (!event.getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to view allowances for event ID %d.",
                    currentUser.getUsername(), currentUser.getId(), eventId);
            throw new SecurityException("User is not authorized to view allowances for this event");
        }
        List<EventUserAllowancesDto> result =
                eventUserAllowanceRepository.findByEventId(eventId).stream()
                        .map(EventUserAllowancesDto::new)
                        .collect(Collectors.toList());
        LOG.infof(
                "Retrieved %d reservation allowances for event ID %d by user: %s (ID: %d)",
                result.size(), eventId, currentUser.getUsername(), currentUser.getId());
        return result;
    }

    /**
     * Deletes a reservation allowance. Access control: The user must be the manager of the event
     * associated with the allowance or have the ADMIN role to delete it.
     *
     * @param id The ID of the reservation allowance to be deleted.
     * @param currentUser The currently authenticated user.
     * @throws EventNotFoundException If the reservation allowance with the specified ID is not
     *     found.
     * @throws SecurityException If the user is not authorized to delete this allowance.
     */
    @Transactional
    public void deleteReservationAllowance(Long id, User currentUser)
            throws EventNotFoundException, SecurityException {
        LOG.debugf(
                "Attempting to delete reservation allowance with ID: %d for user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
        EventUserAllowance allowance =
                eventUserAllowanceRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> {
                                    LOG.warnf(
                                            "Reservation allowance with ID %d not found for"
                                                    + " deletion by user: %s (ID: %d)",
                                            id, currentUser.getUsername(), currentUser.getId());
                                    return new EventNotFoundException("Allowance not found");
                                });

        if (!allowance.getEvent().getManager().equals(currentUser)
                && !currentUser.getRoles().contains(Roles.ADMIN)) {
            LOG.warnf(
                    "User %s (ID: %d) is not authorized to delete reservation allowance with ID"
                            + " %d.",
                    currentUser.getUsername(), currentUser.getId(), id);
            throw new SecurityException("User is not authorized to delete this allowance");
        }

        eventUserAllowanceRepository.delete(allowance);
        LOG.infof(
                "Reservation allowance with ID %d deleted successfully by user: %s (ID: %d)",
                id, currentUser.getUsername(), currentUser.getId());
    }

    private Event getEventById(Long id) throws EventNotFoundException {
        LOG.debugf("Attempting to find event by ID: %d", id);
        return eventRepository
                .findByIdOptional(id)
                .orElseThrow(
                        () -> {
                            LOG.warnf("Event with ID %d not found.", id);
                            return new EventNotFoundException("Event with id " + id + " not found");
                        });
    }

    private User getUserById(Long id) throws UserNotFoundException {
        LOG.debugf("Attempting to find user by ID: %d", id);
        return userRepository
                .findByIdOptional(id)
                .orElseThrow(
                        () -> {
                            LOG.warnf("User with ID %d not found.", id);
                            return new UserNotFoundException("User with id " + id + " not found");
                        });
    }
}
