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

import java.io.BufferedReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalEmailTemplateLocatorTest {

    @TempDir Path overrideDir;

    private String readAll(Reader reader) {
        try (BufferedReader br = new BufferedReader(reader)) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void writeOverride(String relativePath, String content) throws Exception {
        Path file = overrideDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    @Test
    void locatesExistingOverrideFile() throws Exception {
        writeOverride("email/reservation-confirmation.html", "<p>OVERRIDDEN {fullName}</p>");
        ExternalEmailTemplateLocator locator = new ExternalEmailTemplateLocator(overrideDir);

        Optional<TemplateLocation> location = locator.locate("email/reservation-confirmation");

        assertTrue(location.isPresent());
        assertEquals("<p>OVERRIDDEN {fullName}</p>", readAll(location.get().read()));
        assertEquals(
                Variant.TEXT_HTML,
                location.get().getVariant().map(Variant::getContentType).orElse(null));
        assertTrue(location.get().getSource().isPresent());
    }

    @Test
    void acceptsIdWithHtmlSuffix() throws Exception {
        writeOverride("email/password-changed.html", "<p>changed</p>");
        ExternalEmailTemplateLocator locator = new ExternalEmailTemplateLocator(overrideDir);

        assertTrue(locator.locate("email/password-changed.html").isPresent());
    }

    @Test
    void returnsEmptyWhenFileMissing() {
        ExternalEmailTemplateLocator locator = new ExternalEmailTemplateLocator(overrideDir);

        assertFalse(locator.locate("email/event-reminder").isPresent());
    }

    @Test
    void ignoresNonEmailTemplates() throws Exception {
        writeOverride("other/some-template.html", "<p>x</p>");
        ExternalEmailTemplateLocator locator = new ExternalEmailTemplateLocator(overrideDir);

        assertFalse(locator.locate("other/some-template").isPresent());
    }

    @Test
    void disabledWhenNoDirectoryConfigured() {
        ExternalEmailTemplateLocator locator = new ExternalEmailTemplateLocator((Path) null);

        assertFalse(locator.locate("email/reservation-confirmation").isPresent());
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        // A traversal attempt must not escape the override directory.
        ExternalEmailTemplateLocator locator = new ExternalEmailTemplateLocator(overrideDir);

        assertFalse(locator.locate("email/../../secret").isPresent());
    }

    @Test
    void hasHigherPriorityThanDefaultLocator() {
        assertTrue(new ExternalEmailTemplateLocator(overrideDir).getPriority() > 1);
    }
}
