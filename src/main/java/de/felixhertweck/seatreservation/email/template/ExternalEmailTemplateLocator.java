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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Lets operators override the bundled email templates at runtime without rebuilding the
 * application.
 *
 * <p>When {@code email.template.override-dir} points to a directory, this locator is consulted
 * before the default classpath locator for any {@code email/*} template. If a matching {@code
 * .html} file exists in that directory it is used instead of the version shipped inside the
 * artifact; otherwise the locator returns empty and Qute falls back to the bundled template.
 *
 * <p>The override files are still ordinary Qute templates (same {@code {#for}}/{@code {#if}} syntax
 * and HTML escaping) — only their source is read from the filesystem. Because they are not visible
 * at build time they are parsed on first use rather than build-time validated, and a running
 * instance must be restarted to pick up changes.
 *
 * <p>Leaving the property unset (the default) disables the locator entirely, so it is a safe no-op.
 */
@Singleton
public class ExternalEmailTemplateLocator implements TemplateLocator {

    private static final Logger LOG = Logger.getLogger(ExternalEmailTemplateLocator.class);

    /** Only template ids under this prefix may be overridden. */
    private static final String EMAIL_PREFIX = "email/";

    private static final String HTML_SUFFIX = ".html";

    /** Consulted before the default classpath locator (whose priority is lower). */
    private static final int PRIORITY = 100;

    private final Path overrideDir;

    @Inject
    public ExternalEmailTemplateLocator(
            @ConfigProperty(name = "email.template.override-dir") Optional<String> overrideDir) {
        this(
                overrideDir
                        .map(String::trim)
                        .filter(dir -> !dir.isEmpty())
                        .map(Path::of)
                        .orElse(null));
    }

    /**
     * Direct constructor taking the resolved override directory. Also used by unit tests.
     *
     * @param overrideDir the directory to read overrides from, or {@code null} to disable
     */
    ExternalEmailTemplateLocator(Path overrideDir) {
        this.overrideDir = overrideDir;
        if (overrideDir != null) {
            LOG.infof("Email template overrides enabled from directory: %s", overrideDir);
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Optional<TemplateLocation> locate(String id) {
        if (overrideDir == null || id == null || !id.startsWith(EMAIL_PREFIX)) {
            return Optional.empty();
        }

        Path file = resolveWithinOverrideDir(id);
        if (file == null || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            return Optional.empty();
        }

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            LOG.debugf("Using external override for email template '%s' from %s", id, file);
            return Optional.of(new FileTemplateLocation(content, file.toUri()));
        } catch (IOException e) {
            LOG.warnf(e, "Failed to read email template override for '%s' from %s", id, file);
            return Optional.empty();
        }
    }

    /**
     * Resolves the template id to a file inside the override directory, appending the {@code .html}
     * suffix when needed and rejecting any path that would escape the directory (path traversal),
     * including via a symlink.
     *
     * @param id the requested template id (already known to start with {@code email/})
     * @return the resolved file path, or {@code null} if it would escape the override directory
     */
    private Path resolveWithinOverrideDir(String id) {
        String relative = id.endsWith(HTML_SUFFIX) ? id : id + HTML_SUFFIX;
        Path base = overrideDir.toAbsolutePath().normalize();
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) {
            LOG.warnf("Rejected email template override outside override directory: %s", id);
            return null;
        }

        // The prefix check above only guards the literal path; resolve symlinks (on the file and on
        // any parent directory) and re-check containment so an override directory containing a
        // symlink to an outside location cannot be used to read arbitrary files.
        if (Files.exists(resolved)) {
            try {
                Path realBase = base.toRealPath();
                Path realResolved = resolved.toRealPath();
                if (!realResolved.startsWith(realBase)) {
                    LOG.warnf(
                            "Rejected email template override outside override directory (symlink"
                                    + " escape): %s",
                            id);
                    return null;
                }
            } catch (IOException e) {
                LOG.warnf(e, "Failed to resolve real path for email template override: %s", id);
                return null;
            }
        }
        return resolved;
    }

    /** A {@link TemplateLocation} backed by the already-read contents of a filesystem template. */
    private static final class FileTemplateLocation implements TemplateLocation {

        private final String content;
        private final URI source;

        private FileTemplateLocation(String content, URI source) {
            this.content = content;
            this.source = source;
        }

        @Override
        public Reader read() {
            return new StringReader(content);
        }

        @Override
        public Optional<Variant> getVariant() {
            return Optional.of(
                    new Variant(Locale.getDefault(), StandardCharsets.UTF_8, Variant.TEXT_HTML));
        }

        @Override
        public Optional<URI> getSource() {
            return Optional.of(source);
        }
    }
}
