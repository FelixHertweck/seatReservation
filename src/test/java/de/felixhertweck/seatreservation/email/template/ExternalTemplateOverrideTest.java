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
package de.felixhertweck.seatreservation.email.template;

import java.util.Map;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

/**
 * Verifies that Quarkus consults {@link ExternalEmailTemplateLocator} for templates injected via
 * {@code @Location}, so a filesystem override actually replaces the bundled template at runtime.
 */
@QuarkusTest
@TestProfile(ExternalTemplateOverrideTest.OverrideProfile.class)
class ExternalTemplateOverrideTest {

    public static class OverrideProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "email.template.override-dir", "src/test/resources/email-template-overrides");
        }
    }

    @Inject
    @Location("email/password-changed")
    Template passwordChanged;

    @Inject
    @Location("email/event-reminder")
    Template eventReminder;

    @Test
    void overrideTemplateReplacesBundledOne() {
        String html =
                passwordChanged.data("fullName", "Jane Doe").data("currentYear", "2026").render();

        assertTrue(html.contains("EXTERNAL OVERRIDE"), "override marker should be present");
        assertTrue(html.contains("Jane Doe"), "data should still be rendered by Qute");
    }

    @Test
    void bundledTemplateIsUsedWhenNoOverrideExists() {
        // No override file exists for event-reminder, so the bundled template is used.
        String html =
                eventReminder
                        .data("userName", "jane")
                        .data("fullName", "Jane Doe")
                        .data("eventName", "Concert")
                        .data("eventDate", "2026-07-10")
                        .data("eventTime", "20:00")
                        .data("eventLocation", "Main Hall")
                        .data("seats", java.util.List.of())
                        .data("entranceInfo", "")
                        .data("seatmapLink", "https://example.com/s")
                        .data("eventLink", "https://example.com/e")
                        .data("currentYear", "2026")
                        .render();

        assertFalse(html.contains("EXTERNAL OVERRIDE"));
        assertTrue(html.contains("Reminder: Your Event Starts Soon!"));
    }
}
