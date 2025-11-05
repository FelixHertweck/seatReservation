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
package de.felixhertweck.seatreservation.sanitization;

import org.jboss.logging.Logger;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizerUtils {

    private static final Logger LOG = Logger.getLogger(HtmlSanitizerUtils.class);

    private static final PolicyFactory POLICY_FACTORY =
            new HtmlPolicyBuilder().allowUrlProtocols("mailto").toFactory();

    private HtmlSanitizerUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sanitizes the provided HTML string by removing potentially dangerous elements and attributes.
     *
     * @param unsafeHtml the HTML string to sanitize
     * @return the sanitized HTML string, or null if input is null
     */
    public static String sanitize(String unsafeHtml) {
        LOG.debugf("Attempting to sanitize HTML: %s", unsafeHtml);
        if (unsafeHtml == null) {
            LOG.debug("Input HTML is null, returning null.");
            return null;
        }
        String sanitizedHtml = POLICY_FACTORY.sanitize(unsafeHtml);
        LOG.debugf("Sanitized HTML: %s", sanitizedHtml);
        return sanitizedHtml;
    }
}
