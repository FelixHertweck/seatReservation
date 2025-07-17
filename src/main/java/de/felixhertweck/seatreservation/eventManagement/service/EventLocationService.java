package de.felixhertweck.seatreservation.eventManagement.service;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.security.Roles;

@ApplicationScoped
public class EventLocationService {

    @Inject EventLocationRepository eventLocationRepository;

    /**
     * Retrieves a list of EventLocations belonging to the currently authenticated manager. If the
     * user is an administrator, all EventLocations are returned. Otherwise, only EventLocations
     * whose manager is the current user are returned.
     *
     * @return A list of DTOs representing the EventLocations.
     * @throws ForbiddenException If no authenticated user is found.
     */
    public List<EventLocationResponseDTO> getEventLocationsByCurrentManager(User manager)
            throws ForbiddenException {
        List<EventLocation> eventLocations;
        // Access control: If the user is an ADMIN, all EventLocations are returned.
        // Otherwise, only EventLocations belonging to this manager are returned.
        if (manager.getRoles().contains(Roles.ADMIN)) {
            eventLocations = eventLocationRepository.listAll();
        } else {
            eventLocations = eventLocationRepository.findByManager(manager);
        }
        return eventLocations.stream()
                .map(EventLocationResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new EventLocation and assigns the currently authenticated manager as its creator.
     *
     * @param dto The DTO containing the details of the EventLocation to be created.
     * @return A DTO representing the newly created EventLocation.
     */
    @Transactional
    public EventLocationResponseDTO createEventLocation(EventLocationRequestDTO dto, User manager) {
        EventLocation location =
                new EventLocation(dto.getName(), dto.getAddress(), manager, dto.getCapacity());
        eventLocationRepository.persist(location);
        return new EventLocationResponseDTO(location);
    }

    /**
     * Updates an existing EventLocation. The update is only allowed if the currently authenticated
     * user is the manager of the EventLocation or has the ADMIN role.
     *
     * @param id The ID of the EventLocation to be updated.
     * @param dto The DTO containing the updated details of the EventLocation.
     * @return A DTO representing the updated EventLocation.
     * @throws IllegalArgumentException If the EventLocation with the specified ID is not found.
     * @throws ForbiddenException If the user is not authorized to update the EventLocation.
     */
    @Transactional
    public EventLocationResponseDTO updateEventLocation(
            Long id, EventLocationRequestDTO dto, User manager)
            throws IllegalArgumentException, ForbiddenException {
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "EventLocation with id " + id + " not found"));

        // Access control: Checks if the current user is the manager of the location
        // or if the user has the ADMIN role.
        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new ForbiddenException("User is not the manager of this location");
        }

        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setCapacity(dto.getCapacity());
        eventLocationRepository.persist(location);
        return new EventLocationResponseDTO(location);
    }

    /**
     * Deletes an EventLocation. The deletion is only allowed if the currently authenticated user is
     * the manager of the EventLocation or has the ADMIN role.
     *
     * @param id The ID of the EventLocation to be deleted.
     * @throws IllegalArgumentException If the EventLocation with the specified ID is not found.
     * @throws ForbiddenException If the user is not authorized to delete the EventLocation.
     */
    @Transactional
    public void deleteEventLocation(Long id, User manager)
            throws IllegalArgumentException, ForbiddenException {
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "EventLocation with id " + id + " not found"));

        // Access control: Checks if the current user is the manager of the location
        // or if the user has the ADMIN role.
        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new ForbiddenException("User is not the manager of this location");
        }

        eventLocationRepository.delete(location);
    }
}
