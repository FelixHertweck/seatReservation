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
package de.felixhertweck.seatreservation.security.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.felixhertweck.seatreservation.security.service.AltchaService;
import org.altcha.altcha.v1.Altcha;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/** REST resource for fetching ALTCHA Proof-of-Work challenges. */
@Path("/api/altcha")
@Produces(MediaType.APPLICATION_JSON)
public class AltchaResource {

    private static final Logger LOG = Logger.getLogger(AltchaResource.class);

    @Inject AltchaService altchaService;

    /**
     * Gets a fresh ALTCHA Proof-of-Work challenge.
     *
     * @return Altcha.Challenge with algorithm, challenge, salt, signature, etc.
     */
    @GET
    @Path("/challenge")
    @PermitAll
    @Operation(
            summary = "Generate ALTCHA Challenge",
            description = "Generates a fresh Proof-of-Work challenge for form protection.")
    @APIResponse(responseCode = "200", description = "Challenge generated successfully")
    public Altcha.Challenge getChallenge() {
        LOG.debug("Generating ALTCHA challenge");
        return altchaService.createChallenge();
    }
}
