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
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import de.felixhertweck.seatreservation.common.dto.EventLocationMakerDTO;
import de.felixhertweck.seatreservation.common.dto.SeatDTO;
import de.felixhertweck.seatreservation.management.dto.AreaResponseDTO;
import de.felixhertweck.seatreservation.management.dto.EntranceResponseDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.repository.EventLocationAreaRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationEntranceRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationMarkerRepository;
import de.felixhertweck.seatreservation.model.repository.EventLocationRepository;
import de.felixhertweck.seatreservation.model.repository.SeatRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import org.jboss.logging.Logger;

/**
 * Cache delegate service for EventLocation geometry read data (seats, areas, markers, entrances).
 *
 * <p>Methods here perform the actual repository database queries and are annotated with Quarkus
 * {@link CacheResult} and {@link CacheInvalidate}. The calling services (e.g. {@link SeatService})
 * perform authorization checks prior to delegating to this cache.
 */
@ApplicationScoped
public class SeatmapCacheService {

    private static final Logger LOG = Logger.getLogger(SeatmapCacheService.class);

    public static final String CACHE_SEATS = "seatmap-seats-by-location";
    public static final String CACHE_AREAS = "seatmap-areas-by-location";
    public static final String CACHE_MARKERS = "seatmap-markers-by-location";
    public static final String CACHE_ENTRANCES = "seatmap-entrances-by-location";

    @Inject SeatRepository seatRepository;
    @Inject EventLocationAreaRepository areaRepository;
    @Inject EventLocationMarkerRepository markerRepository;
    @Inject EventLocationEntranceRepository entranceRepository;
    @Inject EventLocationRepository eventLocationRepository;
    @Inject TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    /**
     * Runs the given action after the enclosing transaction commits successfully, so that cache
     * invalidation cannot be raced by a concurrent reader repopulating the cache with pre-commit
     * data. If no transaction is active, the action runs immediately.
     *
     * <p>A narrow cache-aside race remains (stale write racing the invalidation); accepted since
     * this data tolerates staleness.
     *
     * @param action the cache invalidation to defer
     */
    public void runAfterSuccessfulCommit(Runnable action) {
        if (transactionSynchronizationRegistry.getTransactionStatus()
                == Status.STATUS_NO_TRANSACTION) {
            action.run();
            return;
        }
        transactionSynchronizationRegistry.registerInterposedSynchronization(
                new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                        // no-op: only afterCompletion is relevant here
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == Status.STATUS_COMMITTED) {
                            action.run();
                        }
                    }
                });
    }

    @CacheResult(cacheName = CACHE_SEATS)
    public List<SeatDTO> getSeatsByLocation(UUID locationId) {
        LOG.debugf("Cache miss: Fetching seats from DB for location ID: %s", locationId);
        EventLocation location = eventLocationRepository.findByIdOptional(locationId).orElse(null);
        if (location == null) {
            return List.of();
        }
        return seatRepository.findByEventLocation(location).stream().map(SeatDTO::new).toList();
    }

    @CacheInvalidate(cacheName = CACHE_SEATS)
    public void invalidateSeats(UUID locationId) {
        LOG.debugf("Invalidating seats cache for location ID: %s", locationId);
    }

    @CacheResult(cacheName = CACHE_AREAS)
    public List<AreaResponseDTO> getAreasByLocation(UUID locationId) {
        LOG.debugf("Cache miss: Fetching areas from DB for location ID: %s", locationId);
        EventLocation location = eventLocationRepository.findByIdOptional(locationId).orElse(null);
        if (location == null) {
            return List.of();
        }
        return areaRepository.findByEventLocation(location).stream()
                .map(AreaResponseDTO::new)
                .toList();
    }

    @CacheInvalidate(cacheName = CACHE_AREAS)
    public void invalidateAreas(UUID locationId) {
        LOG.debugf("Invalidating areas cache for location ID: %s", locationId);
    }

    @CacheResult(cacheName = CACHE_MARKERS)
    public List<EventLocationMakerDTO> getMarkersByLocation(UUID locationId) {
        LOG.debugf("Cache miss: Fetching markers from DB for location ID: %s", locationId);
        EventLocation location = eventLocationRepository.findByIdOptional(locationId).orElse(null);
        if (location == null) {
            return List.of();
        }
        return markerRepository.findByEventLocation(location).stream()
                .map(EventLocationMakerDTO::new)
                .toList();
    }

    @CacheInvalidate(cacheName = CACHE_MARKERS)
    public void invalidateMarkers(UUID locationId) {
        LOG.debugf("Invalidating markers cache for location ID: %s", locationId);
    }

    @CacheResult(cacheName = CACHE_ENTRANCES)
    public List<EntranceResponseDTO> getEntrancesByLocation(UUID locationId) {
        LOG.debugf("Cache miss: Fetching entrances from DB for location ID: %s", locationId);
        EventLocation location = eventLocationRepository.findByIdOptional(locationId).orElse(null);
        if (location == null) {
            return List.of();
        }
        return entranceRepository.findByEventLocation(location).stream()
                .map(EntranceResponseDTO::new)
                .toList();
    }

    @CacheInvalidate(cacheName = CACHE_ENTRANCES)
    public void invalidateEntrances(UUID locationId) {
        LOG.debugf("Invalidating entrances cache for location ID: %s", locationId);
    }

    @CacheInvalidate(cacheName = CACHE_SEATS)
    @CacheInvalidate(cacheName = CACHE_AREAS)
    @CacheInvalidate(cacheName = CACHE_MARKERS)
    @CacheInvalidate(cacheName = CACHE_ENTRANCES)
    public void invalidateAllGeometryForLocation(UUID locationId) {
        LOG.debugf("Invalidating all geometry caches for location ID: %s", locationId);
    }
}
