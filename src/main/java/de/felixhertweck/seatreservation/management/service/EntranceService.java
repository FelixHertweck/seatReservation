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
package de.felixhertweck.seatreservation.management.service;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.management.dto.EntranceRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceResponseDTO;
import de.felixhertweck.seatreservation.management.exception.EntranceInUseException;
import de.felixhertweck.seatreservation.management.exception.EntranceNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationEntrance;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EntranceService {

    private static final Logger LOG = Logger.getLogger(EntranceService.class);

    @Inject EventLocationEntranceRepository entranceRepository;

    @Inject EventLocationAccessService eventLocationAccessService;

    @Inject SeatRepository seatRepository;

    /**
     * Finds all entrances of an event location, verifying the manager owns that location.
     *
     * @param eventLocationId the event location to list entrances for
     * @param manager the manager attempting to access the entrances
     * @return the entrances of the event location
     */
    public List<EntranceResponseDTO> findEntrancesByLocation(
            Long eventLocationId, AuthenticatedUser manager) {
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(eventLocationId, manager);
        return entranceRepository.findByEventLocation(eventLocation).stream()
                .map(EntranceResponseDTO::new)
                .toList();
    }

    /**
     * Finds an entrance by its ID for a given manager, verifying ownership.
     *
     * @param id the entrance ID to retrieve
     * @param manager the manager attempting to access the entrance
     * @return the entrance DTO
     */
    public EntranceResponseDTO findEntranceByIdForManager(Long id, AuthenticatedUser manager) {
        return new EntranceResponseDTO(findEntranceEntityById(id, manager));
    }

    /**
     * Creates a new entrance for the specified event location by a manager.
     *
     * @param dto the entrance request DTO containing entrance details
     * @param manager the manager attempting to create the entrance
     * @return the created entrance DTO
     */
    @Transactional
    public EntranceResponseDTO createEntrance(EntranceRequestDTO dto, AuthenticatedUser manager) {
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Entrance name cannot be empty");
        }

        EventLocationEntrance entrance = new EventLocationEntrance(dto.getName().trim());
        entrance.setEventLocation(eventLocation);
        entranceRepository.persist(entrance);
        LOG.infof(
                "Entrance ID: %d created successfully for event location ID %d",
                entrance.id, eventLocation.getId());
        return new EntranceResponseDTO(entrance);
    }

    /**
     * Updates an existing entrance for the specified event location by a manager.
     *
     * @param id the entrance ID to update
     * @param dto the entrance request DTO containing updated entrance details
     * @param manager the manager attempting to update the entrance
     * @return the updated entrance DTO
     * @throws EntranceInUseException if the entrance would be moved to a different event location
     *     while still referenced by at least one seat
     */
    @Transactional
    public EntranceResponseDTO updateEntrance(
            Long id, EntranceRequestDTO dto, AuthenticatedUser manager) {
        EventLocationEntrance entrance = findEntranceEntityById(id, manager);
        EventLocation newEventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Entrance name cannot be empty");
        }

        // Seats may only reference an entrance of their own event location (see
        // SeatService#resolveEntrance). Moving a referenced entrance elsewhere would break that
        // invariant.
        if (!entrance.getEventLocation().getId().equals(newEventLocation.getId())
                && seatRepository.countByEntrance(entrance) > 0) {
            throw new EntranceInUseException(
                    "Entrance with id "
                            + id
                            + " cannot be moved to another event location while it is still"
                            + " referenced by at least one seat");
        }

        entrance.setName(dto.getName().trim());
        entrance.setEventLocation(newEventLocation);
        entranceRepository.persist(entrance);
        LOG.infof("Entrance ID: %d updated successfully", entrance.id);
        return new EntranceResponseDTO(entrance);
    }

    /**
     * Deletes entrances by their IDs for a given manager, rejecting the deletion if any entrance is
     * still referenced by a seat.
     *
     * @param ids list of entrance IDs to delete
     * @param manager the manager attempting to delete the entrances
     */
    @Transactional
    public void deleteEntrances(List<Long> ids, AuthenticatedUser manager) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No entrance IDs provided for deletion");
        }
        for (Long id : ids) {
            EventLocationEntrance entrance = findEntranceEntityById(id, manager);
            if (seatRepository.countByEntrance(entrance) > 0) {
                throw new EntranceInUseException(
                        "Entrance with id " + id + " is still referenced by at least one seat");
            }
            entranceRepository.delete(entrance);
            LOG.infof("Entrance ID: %d deleted successfully", id);
        }
    }

    /**
     * Finds an entrance entity by its ID, verifying the manager owns its event location.
     *
     * @param id the entrance ID to find
     * @param manager the manager attempting to access the entrance
     * @return the entrance entity
     */
    private EventLocationEntrance findEntranceEntityById(Long id, AuthenticatedUser manager) {
        EventLocationEntrance entrance =
                entranceRepository
                        .findByIdWithEventLocation(id)
                        .orElseThrow(
                                () ->
                                        new EntranceNotFoundException(
                                                "Entrance with id " + id + " not found"));
        eventLocationAccessService.requireAccess(entrance.getEventLocation(), manager);
        return entrance;
    }
}
