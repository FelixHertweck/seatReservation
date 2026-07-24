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
package de.felixhertweck.seatreservation.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Applies {@code V1__baseline.sql} to a fresh database, seeds it with legacy rows carrying bigint
 * ids and foreign keys (simulating a real pre-UUID production database), then applies {@code
 * V2__migrate_to_uuid.sql} and verifies every relationship survived the id swap.
 *
 * <p>Unlike {@link UuidMigrationSchemaValidationTest}, this test does not boot the application at
 * all -- it drives Flyway directly against a disposable Testcontainers Postgres, so it can run V1
 * and V2 as two separate steps with real data seeded in between. That is the one thing schema
 * validation alone can never catch: a wrong join in one of V2's foreign-key backfills would still
 * produce a structurally valid schema, just with rows silently pointing at the wrong parent.
 */
@Testcontainers
class UuidMigrationDataIntegrityTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    private static Flyway flyway(MigrationVersion target) {
        var config =
                Flyway.configure()
                        .dataSource(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword())
                        .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    @Test
    void migrationPreservesRelationshipsFromLegacyBigintData() throws Exception {
        flyway(MigrationVersion.fromVersion("1")).migrate();

        try (Connection conn =
                        DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword());
                Statement st = conn.createStatement()) {
            st.execute(
                    """
INSERT INTO users (id, username, email, emailverified, emailverificationsent, passwordhash, passwordsalt, firstname, lastname)
VALUES (1,'alice','alice@localhost',true,true,'h','s','Alice','A'),
       (2,'bob','bob@localhost',true,true,'h','s','Bob','B')
""");
            st.execute(
                    "INSERT INTO eventlocations (id, name, address, manager_id, capacity) VALUES"
                            + " (10,'City Hall','Main St',2,100)");
            st.execute(
                    "INSERT INTO event_location_entrances (id, event_location_id, name) VALUES"
                            + " (20,10,'Main Entrance')");
            st.execute(
                    "INSERT INTO event_location_areas (id, event_location_id, name) VALUES"
                            + " (30,10,'Ground Floor')");
            st.execute(
                    """
INSERT INTO events (id, name, starttime, endtime, remindersent, event_location_id, manager_id)
VALUES (50,'Concert','2026-01-01 19:00:00','2026-01-01 21:00:00',false,10,2)
""");
            st.execute("INSERT INTO event_supervisors (event_id, user_id) VALUES (50,2)");
            st.execute(
                    """
                    INSERT INTO seats (id, seatnumber, location_id, seatrow, entrance_id, area_id)
                    VALUES (70,'A1',10,'A',20,30)
                    """);
            st.execute(
                    """
INSERT INTO reservations (id, user_id, event_id, seat_id, status, checkincode, livestatus)
VALUES (80,1,50,70,'RESERVED','CODE1','NO_SHOW')
""");
        }

        flyway(null).migrate();

        try (Connection conn =
                        DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword());
                Statement st = conn.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT id FROM users WHERE username = 'alice'")) {
                assertTrue(rs.next());
                assertDoesNotThrow(() -> UUID.fromString(rs.getString("id")));
            }

            try (ResultSet rs =
                    st.executeQuery(
                            """
                            SELECT u.username, e.name AS event_name, s.seatnumber, r.status
                            FROM reservations r
                            JOIN users u ON r.user_id = u.id
                            JOIN events e ON r.event_id = e.id
                            JOIN seats s ON r.seat_id = s.id
                            """)) {
                assertTrue(rs.next());
                assertEquals("alice", rs.getString("username"));
                assertEquals("Concert", rs.getString("event_name"));
                assertEquals("A1", rs.getString("seatnumber"));
                assertEquals("RESERVED", rs.getString("status"));
                assertFalse(rs.next());
            }

            try (ResultSet rs =
                    st.executeQuery(
                            """
                            SELECT el.name AS location_name, mgr.username AS manager
                            FROM events e
                            JOIN eventlocations el ON e.event_location_id = el.id
                            JOIN users mgr ON e.manager_id = mgr.id
                            """)) {
                assertTrue(rs.next());
                assertEquals("City Hall", rs.getString("location_name"));
                assertEquals("bob", rs.getString("manager"));
            }

            try (ResultSet rs =
                    st.executeQuery(
                            """
                            SELECT ent.name AS entrance, ar.name AS area
                            FROM seats s
                            JOIN event_location_entrances ent ON s.entrance_id = ent.id
                            JOIN event_location_areas ar ON s.area_id = ar.id
                            """)) {
                assertTrue(rs.next());
                assertEquals("Main Entrance", rs.getString("entrance"));
                assertEquals("Ground Floor", rs.getString("area"));
            }

            try (ResultSet rs =
                    st.executeQuery(
                            """
                            SELECT u.username
                            FROM event_supervisors es
                            JOIN users u ON es.user_id = u.id
                            """)) {
                assertTrue(rs.next());
                assertEquals("bob", rs.getString("username"));
            }
        }
    }
}
