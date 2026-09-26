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

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixhertweck.seatreservation.common.exception.ValidationException;
import de.felixhertweck.seatreservation.management.dto.AreaRequestDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceRequestDTO;
import de.felixhertweck.seatreservation.management.dto.LayoutBatchRequestDTO;
import de.felixhertweck.seatreservation.management.dto.LayoutBatchResponseDTO;
import de.felixhertweck.seatreservation.management.dto.LayoutEntityType;
import de.felixhertweck.seatreservation.management.dto.LayoutOperationAction;
import de.felixhertweck.seatreservation.management.dto.LayoutOperationDTO;
import de.felixhertweck.seatreservation.management.dto.LayoutOperationResultDTO;
import de.felixhertweck.seatreservation.management.dto.MakerRequestDTO;
import de.felixhertweck.seatreservation.management.dto.SeatRequestDTO;
import de.felixhertweck.seatreservation.model.entity.LayoutChangeLog;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import de.felixhertweck.seatreservation.model.repository.LayoutChangeLogRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import de.felixhertweck.seatreservation.utils.AuthenticatedUser;
import org.jboss.logging.Logger;

/**
 * Applies an ordered list of layout operations to one event location. Every operation is delegated
 * to the regular entity service, so validation, access checks, reservation handling and cache
 * invalidation behave exactly as for the single-entity endpoints. All operations share one
 * transaction: either the whole batch is committed, or nothing is. Each applied step is recorded in
 * {@link LayoutChangeLog} in that same transaction.
 */
@ApplicationScoped
public class LayoutBatchService {

    private static final Logger LOG = Logger.getLogger(LayoutBatchService.class);

    @Inject EventLocationAccessService eventLocationAccessService;
    @Inject EventLocationService eventLocationService;
    @Inject EntranceService entranceService;
    @Inject AreaService areaService;
    @Inject MarkerService markerService;
    @Inject SeatService seatService;
    @Inject SeatRepository seatRepository;
    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationMarkerRepository markerRepository;
    @Inject LayoutChangeLogRepository changeLogRepository;
    @Inject ObjectMapper objectMapper;

    @Transactional
    public LayoutBatchResponseDTO apply(
            UUID locationId, LayoutBatchRequestDTO request, AuthenticatedUser manager) {
        eventLocationAccessService.findOwnedEventLocation(locationId, manager);

        UUID batchId = UUID.randomUUID();
        Map<LayoutEntityType, Map<String, UUID>> refs = new EnumMap<>(LayoutEntityType.class);
        List<LayoutOperationResultDTO> results = new ArrayList<>();
        int sequenceNo = 0;

        for (LayoutOperationDTO op : request.getOperations()) {
            sequenceNo++;
            try {
                UUID entityId = execute(op, locationId, refs, manager);
                results.add(
                        new LayoutOperationResultDTO(
                                sequenceNo, op.getEntity(), op.getAction(), entityId, op.getRef()));
                changeLogRepository.persist(
                        new LayoutChangeLog(
                                batchId,
                                sequenceNo,
                                locationId,
                                manager.id(),
                                op.getEntity().name(),
                                op.getAction().name(),
                                entityId,
                                op.getRef(),
                                toJson(op),
                                Instant.now()));
            } catch (RuntimeException e) {
                LOG.warnf(
                        "Layout batch %s for location %s failed at operation #%d (%s %s): %s",
                        batchId,
                        locationId,
                        sequenceNo,
                        op.getAction(),
                        op.getEntity(),
                        e.getMessage());
                throw e;
            }
        }

        LOG.infof(
                "Layout batch %s with %d operations applied to location %s by manager ID: %s",
                batchId, results.size(), locationId, manager.id());
        return new LayoutBatchResponseDTO(batchId, results);
    }

    private UUID execute(
            LayoutOperationDTO op,
            UUID locationId,
            Map<LayoutEntityType, Map<String, UUID>> refs,
            AuthenticatedUser manager) {
        LayoutEntityType entity = op.getEntity();
        LayoutOperationAction action = op.getAction();
        UUID id = op.getId();
        if (action != LayoutOperationAction.CREATE) {
            if (id == null && entity != LayoutEntityType.LOCATION) {
                throw new ValidationException("Operation " + action + " requires an id");
            }
            if (entity != LayoutEntityType.LOCATION) {
                requireBelongsToLocation(entity, id, locationId);
            }
        }
        if (action == LayoutOperationAction.CREATE && op.getRef() != null) {
            Map<String, UUID> known = refs.computeIfAbsent(entity, k -> new HashMap<>());
            if (known.containsKey(op.getRef())) {
                throw new ValidationException("Duplicate ref '" + op.getRef() + "' for " + entity);
            }
        }

        UUID resultId =
                switch (entity) {
                    case LOCATION -> executeLocation(op, locationId, manager);
                    case ENTRANCE -> executeEntrance(op, locationId, manager);
                    case AREA -> executeArea(op, locationId, manager);
                    case MARKER -> executeMarker(op, locationId, manager);
                    case SEAT -> executeSeat(op, locationId, refs, manager);
                };

        if (action == LayoutOperationAction.CREATE && op.getRef() != null) {
            refs.get(entity).put(op.getRef(), resultId);
        }
        return resultId;
    }

    private UUID executeLocation(LayoutOperationDTO op, UUID locationId, AuthenticatedUser m) {
        if (op.getAction() != LayoutOperationAction.UPDATE) {
            throw new ValidationException("LOCATION only supports UPDATE");
        }
        if (op.getId() != null && !op.getId().equals(locationId)) {
            throw new ValidationException("Location id does not match the batch's location");
        }
        return eventLocationService
                .updateEventLocation(locationId, require(op.getLocation(), op), m)
                .id();
    }

    private UUID executeEntrance(LayoutOperationDTO op, UUID locationId, AuthenticatedUser m) {
        return switch (op.getAction()) {
            case CREATE -> {
                EntranceRequestDTO dto = require(op.getEntrance(), op);
                requireLocation(dto.getEventLocationId(), locationId);
                yield entranceService.createEntrance(dto, m).id();
            }
            case UPDATE -> {
                EntranceRequestDTO dto = require(op.getEntrance(), op);
                requireLocation(dto.getEventLocationId(), locationId);
                yield entranceService.updateEntrance(op.getId(), dto, m).id();
            }
            case DELETE -> {
                entranceService.deleteEntrances(List.of(op.getId()), m);
                yield op.getId();
            }
        };
    }

    private UUID executeArea(LayoutOperationDTO op, UUID locationId, AuthenticatedUser m) {
        return switch (op.getAction()) {
            case CREATE -> {
                AreaRequestDTO dto = require(op.getArea(), op);
                requireLocation(dto.getEventLocationId(), locationId);
                yield areaService.createArea(dto, m).id();
            }
            case UPDATE -> {
                AreaRequestDTO dto = require(op.getArea(), op);
                requireLocation(dto.getEventLocationId(), locationId);
                yield areaService.updateArea(op.getId(), dto, m).id();
            }
            case DELETE -> {
                areaService.deleteAreas(List.of(op.getId()), m);
                yield op.getId();
            }
        };
    }

    private UUID executeMarker(LayoutOperationDTO op, UUID locationId, AuthenticatedUser m) {
        return switch (op.getAction()) {
            case CREATE -> {
                MakerRequestDTO dto = require(op.getMarker(), op);
                requireLocation(dto.getEventLocationId(), locationId);
                yield markerService.createMarker(dto, m).id();
            }
            case UPDATE -> {
                MakerRequestDTO dto = require(op.getMarker(), op);
                requireLocation(dto.getEventLocationId(), locationId);
                yield markerService.updateMarker(op.getId(), dto, m).id();
            }
            case DELETE -> {
                markerService.deleteMarkers(List.of(op.getId()), m);
                yield op.getId();
            }
        };
    }

    private UUID executeSeat(
            LayoutOperationDTO op,
            UUID locationId,
            Map<LayoutEntityType, Map<String, UUID>> refs,
            AuthenticatedUser m) {
        return switch (op.getAction()) {
            case CREATE -> seatService.createSeatManager(prepareSeat(op, locationId, refs), m).id();
            case UPDATE ->
                    seatService
                            .updateSeatForManager(op.getId(), prepareSeat(op, locationId, refs), m)
                            .id();
            case DELETE -> {
                seatService.deleteSeatForManager(List.of(op.getId()), m);
                yield op.getId();
            }
        };
    }

    /** Checks the target location and substitutes refs to entities created earlier in the batch. */
    private SeatRequestDTO prepareSeat(
            LayoutOperationDTO op, UUID locationId, Map<LayoutEntityType, Map<String, UUID>> refs) {
        SeatRequestDTO dto = require(op.getSeat(), op);
        requireLocation(dto.getEventLocationId(), locationId);
        if (op.getAreaRef() != null) {
            dto.setAreaId(resolveRef(refs, LayoutEntityType.AREA, op.getAreaRef()));
        }
        if (op.getEntranceRef() != null) {
            dto.setEntranceId(resolveRef(refs, LayoutEntityType.ENTRANCE, op.getEntranceRef()));
        }
        return dto;
    }

    private UUID resolveRef(
            Map<LayoutEntityType, Map<String, UUID>> refs, LayoutEntityType type, String ref) {
        UUID id = refs.getOrDefault(type, Map.of()).get(ref);
        if (id == null) {
            throw new ValidationException(
                    type + " ref '" + ref + "' is not defined by an earlier operation");
        }
        return id;
    }

    private <T> T require(T payload, LayoutOperationDTO op) {
        if (payload == null) {
            throw new ValidationException(
                    "Operation " + op.getAction() + " " + op.getEntity() + " requires its payload");
        }
        return payload;
    }

    private void requireLocation(UUID dtoLocationId, UUID locationId) {
        if (!locationId.equals(dtoLocationId)) {
            throw new ValidationException(
                    "eventLocationId of an operation must match the batch's location");
        }
    }

    /**
     * The entity services only check that the manager may access the target, not that it belongs to
     * this batch's location. Without this, a batch for location A could modify or move entities of
     * another location the manager also owns.
     */
    private void requireBelongsToLocation(LayoutEntityType type, UUID id, UUID locationId) {
        UUID actual =
                switch (type) {
                    case ENTRANCE ->
                            entranceRepository
                                    .findByIdWithEventLocation(id)
                                    .map(e -> e.getEventLocation().getId())
                                    .orElse(null);
                    case AREA ->
                            areaRepository
                                    .findByIdWithEventLocation(id)
                                    .map(a -> a.getEventLocation().getId())
                                    .orElse(null);
                    case MARKER ->
                            markerRepository
                                    .findByIdWithEventLocation(id)
                                    .map(m -> m.getEventLocation().getId())
                                    .orElse(null);
                    case SEAT ->
                            seatRepository.findByIdsWithLocation(List.of(id)).stream()
                                    .findFirst()
                                    .map(Seat::getLocation)
                                    .map(l -> l.getId())
                                    .orElse(null);
                    case LOCATION -> locationId;
                };
        // Unknown ids fall through to the entity service, which raises its own NotFound.
        if (actual != null && !actual.equals(locationId)) {
            throw new ValidationException(
                    type + " " + id + " does not belong to the batch's location");
        }
    }

    private String toJson(LayoutOperationDTO op) {
        try {
            return objectMapper.writeValueAsString(op);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize layout operation", e);
        }
    }
}
