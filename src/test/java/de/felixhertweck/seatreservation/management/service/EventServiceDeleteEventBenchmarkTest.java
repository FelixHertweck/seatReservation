/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EventRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EventServiceDeleteEventBenchmarkTest {

    @Mock EventRepository eventRepository;

    @InjectMocks EventService eventService;

    User managerUser;

    int numEvents = 1000;
    List<Long> eventIds;
    List<Event> mockEvents;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        managerUser = new User();
        managerUser.id = 2L;
        managerUser.setRoles(Set.of(Roles.USER));

        eventIds = LongStream.range(1, numEvents + 1).boxed().collect(Collectors.toList());
        mockEvents = new ArrayList<>();

        for (Long id : eventIds) {
            Event event = new Event();
            event.id = id;
            event.setName("Event " + id);
            event.setManager(managerUser);
            mockEvents.add(event);
            when(eventRepository.findByIdOptional(id)).thenReturn(Optional.of(event));
        }

        PanacheQuery<Event> queryMock = mock(PanacheQuery.class);
        when(queryMock.list()).thenReturn(mockEvents);
        when(eventRepository.find(
                        eq("from Event e left join fetch e.manager where e.id in ?1"), anyList()))
                .thenReturn(queryMock);
    }

    @Test
    void deleteEvent_Benchmark() throws Exception {
        // Warmup
        eventService.deleteEvent(eventIds.subList(0, 10), managerUser);

        long startTime = System.nanoTime();

        eventService.deleteEvent(eventIds, managerUser);

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("===== BENCHMARK RESULTS =====");
        System.out.println("Time to delete " + numEvents + " events: " + durationMs + " ms");
        System.out.println("=============================");
    }
}
