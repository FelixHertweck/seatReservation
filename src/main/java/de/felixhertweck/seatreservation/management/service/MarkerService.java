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

import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.management.dto.MakerRequestDTO;
import de.felixhertweck.seatreservation.management.exception.MarkerNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MarkerService {

    private static final Logger LOG = Logger.getLogger(MarkerService.class);

    @Inject EventLocationMarkerRepository markerRepository;

    @Inject EventLocationAccessService eventLocationAccessService;

    /**
     * Finds all markers of an event location, verifying the manager owns that location.
     *
     * @param eventLocationId the event location to list markers for
     * @param manager the manager attempting to access the markers
     * @return the markers of the event location
     */
    public List<EventLocationMakerDTO> findMarkersByLocation(Long eventLocationId, User manager) {
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(eventLocationId, manager);
        return markerRepository.findByEventLocation(eventLocation).stream()
                .map(EventLocationMakerDTO::new)
                .toList();
    }

    /**
     * Finds a marker by its ID for a given manager, verifying ownership.
     *
     * @param id the marker ID to retrieve
     * @param manager the manager attempting to access the marker
     * @return the marker DTO
     */
    public EventLocationMakerDTO findMarkerByIdForManager(Long id, User manager) {
        return new EventLocationMakerDTO(findMarkerEntityById(id, manager));
    }

    /**
     * Creates a new marker for the specified event location by a manager.
     *
     * @param dto the marker request DTO containing marker details
     * @param manager the manager attempting to create the marker
     * @return the created marker DTO
     */
    @Transactional
    public EventLocationMakerDTO createMarker(MakerRequestDTO dto, User manager) {
        EventLocation eventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);
        if (dto.getLabel() == null || dto.getLabel().trim().isEmpty()) {
            throw new IllegalArgumentException("Marker label cannot be empty");
        }

        EventLocationMarker marker =
                new EventLocationMarker(
                        dto.getLabel(),
                        dto.getCoordinate().xCoordinate(),
                        dto.getCoordinate().yCoordinate());
        marker.setEventLocation(eventLocation);
        markerRepository.persist(marker);
        LOG.infof(
                "Marker ID: %d created successfully for event location ID %d",
                marker.id, eventLocation.getId());
        return new EventLocationMakerDTO(marker);
    }

    /**
     * Updates an existing marker for the specified event location by a manager.
     *
     * @param id the marker ID to update
     * @param dto the marker request DTO containing updated marker details
     * @param manager the manager attempting to update the marker
     * @return the updated marker DTO
     */
    @Transactional
    public EventLocationMakerDTO updateMarker(Long id, MakerRequestDTO dto, User manager) {
        EventLocationMarker marker = findMarkerEntityById(id, manager);
        EventLocation newEventLocation =
                eventLocationAccessService.findOwnedEventLocation(
                        dto.getEventLocationId(), manager);
        if (dto.getLabel() == null || dto.getLabel().trim().isEmpty()) {
            throw new IllegalArgumentException("Marker label cannot be empty");
        }

        marker.setLabel(dto.getLabel());
        marker.setCoordinate(dto.getCoordinate().toEntity());
        marker.setEventLocation(newEventLocation);
        markerRepository.persist(marker);
        LOG.infof("Marker ID: %d updated successfully", marker.id);
        return new EventLocationMakerDTO(marker);
    }

    /**
     * Deletes markers by their IDs for a given manager. Markers are never referenced by another
     * entity, so deletion is always unconditionally allowed once ownership is verified.
     *
     * @param ids list of marker IDs to delete
     * @param manager the manager attempting to delete the markers
     */
    @Transactional
    public void deleteMarkers(List<Long> ids, User manager) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No marker IDs provided for deletion");
        }
        for (Long id : ids) {
            EventLocationMarker marker = findMarkerEntityById(id, manager);
            markerRepository.delete(marker);
            LOG.infof("Marker ID: %d deleted successfully", id);
        }
    }

    /**
     * Finds a marker entity by its ID, verifying the manager owns its event location.
     *
     * @param id the marker ID to find
     * @param manager the manager attempting to access the marker
     * @return the marker entity
     */
    private EventLocationMarker findMarkerEntityById(Long id, User manager) {
        EventLocationMarker marker =
                markerRepository
                        .findByIdWithEventLocation(id)
                        .orElseThrow(
                                () ->
                                        new MarkerNotFoundException(
                                                "Marker with id " + id + " not found"));
        eventLocationAccessService.requireAccess(marker.getEventLocation(), manager);
        return marker;
    }
}
