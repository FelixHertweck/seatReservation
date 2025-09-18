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
package de.felixhertweck.seatreservation.model.repository;

import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLocationRepository implements PanacheRepository<EventLocation> {
    private static final Logger LOG = Logger.getLogger(EventLocationRepository.class);

    public List<EventLocation> findByManager(User manager) {
        return find("manager", manager).list();
    }

    public Optional<EventLocation> findByIdWithRelations(Long id) {
        LOG.debugf("Finding for event location: %s", id);
        return find(
                        "SELECT e FROM EventLocation e JOIN FETCH e.manager WHERE e.id = :id",
                        Parameters.with("id", id))
                .firstResultOptional();
    }
}
