package de.felixhertweck.seatreservation.eventManagement.service;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.dto.EventLocationResponseDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRegistrationDTO;
import de.felixhertweck.seatreservation.eventManagement.dto.EventLocationRequestDTO;
import de.felixhertweck.seatreservation.eventManagement.exception.EventLocationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
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
     */
    public List<EventLocationResponseDTO> getEventLocationsByCurrentManager(User manager) {
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
    public EventLocationResponseDTO createEventLocation(EventLocationRequestDTO dto, User manager)
            throws IllegalArgumentException {
        if (dto == null
                || dto.getName() == null
                || dto.getName().trim().isEmpty()
                || dto.getAddress() == null
                || dto.getAddress().trim().isEmpty()
                || dto.getCapacity() == null
                || dto.getCapacity() <= 0) {
            throw new IllegalArgumentException("Invalid EventLocation data provided.");
        }

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
     * @throws SecurityException If the user is not authorized to update the EventLocation.
     */
    @Transactional
    public EventLocationResponseDTO updateEventLocation(
            Long id, EventLocationRequestDTO dto, User manager)
            throws IllegalArgumentException, SecurityException {
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new EventLocationNotFoundException(
                                                "EventLocation with id " + id + " not found"));

        // Access control: Checks if the current user is the manager of the location
        // or if the user has the ADMIN role.
        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not the manager of this location");
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
     * @throws SecurityException If the user is not authorized to delete the EventLocation.
     */
    @Transactional
    public void deleteEventLocation(Long id, User manager)
            throws IllegalArgumentException, SecurityException {
        EventLocation location =
                eventLocationRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () ->
                                        new EventLocationNotFoundException(
                                                "EventLocation with id " + id + " not found"));

        // Access control: Checks if the current user is the manager of the location
        // or if the user has the ADMIN role.
        if (!location.getManager().id.equals(manager.getId())
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new SecurityException("User is not the manager of this location");
        }

        eventLocationRepository.delete(location);
    }

    /**
     * Creates a new EventLocation with seats. The manager of the EventLocation is set to the
     * currently authenticated user.
     *
     * @param dto The DTO containing the details of the EventLocation and its seats.
     * @param manager The currently authenticated user who is the manager of the EventLocation.
     * @throws IllegalArgumentException If the provided data is invalid.
     * @throws SecurityException If the user is not authorized to create the EventLocation.
     * @return A DTO representing the newly created EventLocation with its seats.
     */
    @Transactional
    public EventLocationResponseDTO createEventLocationWithSeats(
            EventLocationRegistrationDTO dto, User manager)
            throws IllegalArgumentException, SecurityException {
        EventLocationRegistrationDTO.EventLocationData locationData = dto.getEventLocation();
        if (locationData == null
                || locationData.getName() == null
                || locationData.getName().trim().isEmpty()
                || locationData.getAddress() == null
                || locationData.getAddress().trim().isEmpty()
                || locationData.getCapacity() == null
                || locationData.getCapacity() <= 0) {
            throw new IllegalArgumentException("Invalid EventLocation data provided.");
        }

        EventLocation location =
                new EventLocation(
                        locationData.getName(),
                        locationData.getAddress(),
                        manager,
                        locationData.getCapacity());
        eventLocationRepository.persist(location);

        if (dto.getSeats() != null) {
            for (EventLocationRegistrationDTO.SeatData seatData : dto.getSeats()) {
                if (seatData.getSeatNumber() == null || seatData.getSeatNumber().trim().isEmpty()) {
                    throw new IllegalArgumentException("Seat number cannot be empty");
                }
                if (seatData.getXCoordinate() < 0 || seatData.getYCoordinate() < 0) {
                    throw new IllegalArgumentException("Coordinates cannot be negative");
                }
                Seat seat =
                        new Seat(
                                seatData.getSeatNumber(),
                                location,
                                seatData.getXCoordinate(),
                                seatData.getYCoordinate());
                location.getSeats().add(seat);
            }
        }

        eventLocationRepository.persist(location);
        return new EventLocationResponseDTO(location);
    }
}
