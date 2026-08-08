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

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

/**
 * Boots the application with {@code hibernate-orm.schema-management.strategy=validate} and Flyway
 * migrating at start -- exactly how the docker/prod profile is configured. If {@code
 * V1__baseline.sql} + {@code V2__migrate_to_uuid.sql} ever drift from what the current entity model
 * expects (a renamed column, a changed constraint, a forgotten migration for a new field),
 * Hibernate's own schema validator rejects it and the Quarkus application fails to start -- which
 * fails this test before the single assertion below is ever reached.
 */
@QuarkusTest
@TestProfile(UuidMigrationSchemaValidationTest.MigratedSchemaProfile.class)
class UuidMigrationSchemaValidationTest {

    @Inject UserRepository userRepository;

    @Test
    void applicationStartsAgainstTheMigratedSchema() {
        // Reaching this line at all is the real assertion: if V1+V2 no longer produced exactly
        // what the entity model expects, Hibernate's `validate` strategy would have thrown during
        // Quarkus's startup, failing this test before the method body ever ran.
        assertTrue(true);
    }

    @Test
    @Transactional
    void migrationSeedsTheBoxofficeSystemUser() {
        // V8__add_boxoffice_user.sql must have run as part of the same migrate-at-start pass;
        // this also implicitly validates its column list against the Hibernate-validated schema.
        User boxofficeUser = userRepository.findByUsername("boxoffice");
        assertNotNull(boxofficeUser);
        assertNull(boxofficeUser.getPasswordHash());
        assertNull(boxofficeUser.getEmail());
    }

    public static class MigratedSchemaProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.hibernate-orm.schema-management.strategy", "validate",
                    "quarkus.hibernate-orm.sql-load-script", "no-file",
                    "quarkus.flyway.migrate-at-start", "true",
                    "quarkus.flyway.baseline-on-migrate", "true",
                    "quarkus.flyway.baseline-version", "1");
        }
    }
}
