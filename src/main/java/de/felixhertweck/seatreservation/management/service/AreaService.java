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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.management.dto.AreaRequestDTO;
import de.felixhertweck.seatreservation.management.dto.AreaResponseDTO;
import de.felixhertweck.seatreservation.management.exception.AreaInUseException;
import de.felixhertweck.seatreservation.management.exception.AreaNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationArea;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AreaService {

    private static final Logger LOG = Logger.getLogger(AreaService.class);

    @Inject EventLocationAreaRepository areaRepository;

    @Inject EventLocationAccessService eventLocationAccessService;

    @Inject SeatRepository seatRepository;

    @Inject SeatmapCacheService seatmapCacheService;

    /**
     * Finds all areas of an event location, verifying the manager owns that location.
     *
     * @param eventLocationId the event location to list areas for
     * @param manager the manager attempting to access the areas
     * @return the areas of the event location
     */
    public List<AreaResponseDTO> findAreasByLocation(
            UUID eventLocationId, AuthenticatedUser manager) {
        eventLocationAccessService.findOwnedEventLocation(eventLocationId, manager);
        return seatmapCacheService.getAreasByLocation(eventLocationId);
    }

    /**
     * Finds an area by its ID for a given manager, verifying ownership.
     *
     * @param id the area ID to retrieve
     * @param manager the manager attempting to access the area
     * @return the area DTO
     */
    public AreaResponseDTO findAreaByIdForManager(UUID id, AuthenticatedUser manager) {
        return new AreaResponseDTO(findAreaEntityById(id, manager));
    }

    /**
     * Creates a new area for the specified event location by a manager.
     *
     * @param dto the area request DTO containing area details
     * @param manager the manager attempting to create the area
     * @return the created area DTO
     */
    @Transactional
    public AreaResponseDTO createArea(AreaRequestDTO dto, AuthenticatedUser manager) {
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Area name cannot be empty");
        }

        EventLocationArea area = new EventLocationArea(dto.getName().trim());
        area.setEventLocation(eventLocation);
        area.setBoundary(
                dto.getBoundary() == null
                        ? new ArrayList<>()
                        : dto.getBoundary().stream()
                                .map(CoordinateDTO::toEntity)
                                .collect(Collectors.toCollection(ArrayList::new)));
        areaRepository.persist(area);
        seatmapCacheService.runAfterSuccessfulCommit(
                () -> seatmapCacheService.invalidateAreas(eventLocation.getId()));
        LOG.infof(
                "Area ID: %s created successfully for event location ID %s",
                area.id, eventLocation.getId());
        return new AreaResponseDTO(area);
    }

    /**
     * Updates an existing area for the specified event location by a manager.
     *
     * @param id the area ID to update
     * @param dto the area request DTO containing updated area details
     * @param manager the manager attempting to update the area
     * @return the updated area DTO
     * @throws AreaInUseException if the area would be moved to a different event location while
     *     still referenced by at least one seat
     */
    @Transactional
    public AreaResponseDTO updateArea(UUID id, AreaRequestDTO dto, AuthenticatedUser manager) {
        EventLocationArea area = findAreaEntityById(id, manager);
        UUID oldLocationId = area.getEventLocation().getId();
        EventLocation newEventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Area name cannot be empty");
        }

        // Seats may only reference an area of their own event location (see
        // SeatService#resolveArea). Moving a referenced area elsewhere would break that invariant.
        if (!area.getEventLocation().getId().equals(newEventLocation.getId())
                && seatRepository.countByArea(area) > 0) {
            throw new AreaInUseException(
                    "Area with id "
                            + id
                            + " cannot be moved to another event location while it is still"
                            + " referenced by at least one seat");
        }

        area.setName(dto.getName().trim());
        area.setEventLocation(newEventLocation);
        area.setBoundary(
                dto.getBoundary() == null
                        ? new ArrayList<>()
                        : dto.getBoundary().stream()
                                .map(CoordinateDTO::toEntity)
                                .collect(Collectors.toCollection(ArrayList::new)));
        areaRepository.persist(area);
        UUID newLocationId = newEventLocation.getId();
        seatmapCacheService.runAfterSuccessfulCommit(
                () -> {
                    seatmapCacheService.invalidateAreas(oldLocationId);
                    // SeatDTO denormalizes the area name.
                    seatmapCacheService.invalidateSeats(oldLocationId);
                    if (!oldLocationId.equals(newLocationId)) {
                        seatmapCacheService.invalidateAreas(newLocationId);
                    }
                });
        LOG.infof("Area ID: %s updated successfully", area.id);
        return new AreaResponseDTO(area);
    }

    /**
     * Deletes areas by their IDs for a given manager, rejecting the deletion if any area is still
     * referenced by a seat.
     *
     * @param ids list of area IDs to delete
     * @param manager the manager attempting to delete the areas
     */
    @Transactional
    public void deleteAreas(List<UUID> ids, AuthenticatedUser manager) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No area IDs provided for deletion");
        }

        Map<UUID, EventLocationArea> areaMap =
                areaRepository.findByIdsWithEventLocation(ids).stream()
                        .collect(Collectors.toMap(a -> a.id, a -> a));
        Set<UUID> usedAreaIds = new HashSet<>(seatRepository.findUsedAreaIds(ids));

        Set<UUID> locationIdsToInvalidate = new HashSet<>();
        for (UUID id : ids) {
            EventLocationArea area = areaMap.get(id);
            if (area == null) {
                throw new AreaNotFoundException("Area with id " + id + " not found");
            }
            eventLocationAccessService.requireAccess(area.getEventLocation(), manager);
            if (usedAreaIds.contains(id)) {
                throw new AreaInUseException(
                        "Area with id " + id + " is still referenced by at least one seat");
            }
            areaRepository.delete(area);
            locationIdsToInvalidate.add(area.getEventLocation().getId());
            LOG.infof("Area ID: %s deleted successfully", id);
        }
        if (!locationIdsToInvalidate.isEmpty()) {
            seatmapCacheService.runAfterSuccessfulCommit(
                    () -> locationIdsToInvalidate.forEach(seatmapCacheService::invalidateAreas));
        }
    }

    /**
     * Finds an area entity by its ID, verifying the manager owns its event location.
     *
     * @param id the area ID to find
     * @param manager the manager attempting to access the area
     * @return the area entity
     */
    private EventLocationArea findAreaEntityById(UUID id, AuthenticatedUser manager) {
        EventLocationArea area =
                areaRepository
                        .findByIdWithEventLocation(id)
                        .orElseThrow(
                                () ->
                                        new AreaNotFoundException(
                                                "Area with id " + id + " not found"));
        eventLocationAccessService.requireAccess(area.getEventLocation(), manager);
        return area;
    }
}
