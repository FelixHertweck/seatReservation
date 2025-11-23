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
package de.felixhertweck.seatreservation.wallet.resource;

import java.net.URI;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.wallet.service.GoogleWalletService;
import org.jboss.logging.Logger;

@Path("/api/wallet/pass")
public class GoogleWalletResource {

    private static final Logger LOG = Logger.getLogger(GoogleWalletResource.class);

    @Inject GoogleWalletService googleWalletService;

    @GET
    @PermitAll
    public Response getGoogleWalletPass(@QueryParam("token") String token) {
        if (token == null || token.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token is required").build();
        }

        try {
            String jwt = googleWalletService.createGoogleWalletJwt(token);
            String saveUrl = "https://pay.google.com/gp/v/save/" + jwt;
            return Response.temporaryRedirect(URI.create(saveUrl)).build();
        } catch (IllegalArgumentException e) {
            LOG.warnf("Invalid token for Google Wallet pass: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Invalid or expired token")
                    .build();
        } catch (Exception e) {
            LOG.error("Error generating Google Wallet pass", e);
            return Response.serverError().entity("Internal Server Error").build();
        }
    }
}
