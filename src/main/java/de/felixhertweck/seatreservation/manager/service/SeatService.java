package de.felixhertweck.seatreservation.manager.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.SecurityContext;

import de.felixhertweck.seatreservation.manager.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.manager.dto.SeatResponseDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.security.Roles;

@ApplicationScoped
public class SeatService {

    @Inject SeatRepository seatRepository;

    @Inject EventLocationRepository eventLocationRepository;

    @Inject SecurityContext securityContext;

    @Transactional
    public SeatResponseDTO createSeatManager(SeatRequestDTO dto, User manager) {
        EventLocation eventLocation =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "EventLocation with id "
                                                        + dto.getEventLocationId()
                                                        + " not found"));

        if (!manager.getEventLocations().contains(eventLocation)
                && !manager.getRoles().contains(Roles.ADMIN)) {
            throw new ForbiddenException("Manager does not own this EventLocation");
        }

        Seat seat = new Seat();
        seat.setSeatNumber(dto.getSeatNumber());
        seat.setLocation(eventLocation);
        seatRepository.persist(seat);
        return new SeatResponseDTO(seat);
    }

    public List<SeatResponseDTO> findAllSeatsForManager(User manager) {
        Set<EventLocation> managerLocations = manager.getEventLocations();
        return seatRepository.listAll().stream()
                .filter(seat -> managerLocations.contains(seat.getLocation()))
                .map(SeatResponseDTO::new)
                .collect(Collectors.toList());
    }

    public SeatResponseDTO findSeatByIdForManager(Long id, User manager) {
        Seat seat = findSeatEntityById(id, manager);
        if (!manager.getEventLocations().contains(seat.getLocation())) {
            throw new ForbiddenException("Manager does not own the EventLocation of this seat");
        }
        return new SeatResponseDTO(seat);
    }

    @Transactional
    public SeatResponseDTO updateSeatForManager(Long id, SeatRequestDTO dto, User manager) {
        Seat seat = findSeatEntityById(id, manager);
        if (!manager.getEventLocations().contains(seat.getLocation())) {
            throw new ForbiddenException("Manager does not own the EventLocation of this seat");
        }

        EventLocation newEventLocation =
                eventLocationRepository
                        .findByIdOptional(dto.getEventLocationId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "EventLocation with id "
                                                        + dto.getEventLocationId()
                                                        + " not found"));

        if (!manager.getEventLocations().contains(newEventLocation)) {
            throw new ForbiddenException("Manager does not own the new EventLocation");
        }

        seat.setSeatNumber(dto.getSeatNumber());
        seat.setLocation(newEventLocation);
        seat.setXCoordinate(dto.getXCoordinate());
        seat.setYCoordinate(dto.getYCoordinate());
        seatRepository.persist(seat);
        return new SeatResponseDTO(seat);
    }

    @Transactional
    public void deleteSeatForManager(Long id, User manager) {
        Seat seat = findSeatEntityById(id, manager);
        if (!manager.getEventLocations().contains(seat.getLocation())) {
            throw new ForbiddenException("Manager does not own the EventLocation of this seat");
        }
        seatRepository.delete(seat);
    }

    private Seat findSeatEntityById(Long id, User currentUser) throws ForbiddenException {
        // Check if user has access to linked location
        Seat seat =
                seatRepository
                        .findByIdOptional(id)
                        .orElseThrow(
                                () -> new NotFoundException("Seat with id " + id + " not found"));
        User manager = seat.getLocation().getManager();

        if (!manager.equals(currentUser) && !securityContext.isUserInRole(Roles.ADMIN)) {
            throw new ForbiddenException("You do not have permission to access this seat");
        }

        return seat;
    }
}
