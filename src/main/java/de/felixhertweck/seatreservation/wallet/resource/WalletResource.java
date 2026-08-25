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

import java.util.UUID;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import de.felixhertweck.seatreservation.wallet.dto.WalletConfigDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import de.felixhertweck.seatreservation.wallet.service.WalletPassService;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/user/wallet")
@Consumes(MediaType.APPLICATION_JSON)
public class WalletResource {

    private static final Logger LOG = Logger.getLogger(WalletResource.class);

    @Inject WalletPassService walletPassService;

    @Inject UserSecurityContext userSecurityContext;

    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = WalletConfigDTO.class)))
    public Response getWalletConfig() {
        return Response.ok(
                        new WalletConfigDTO(
                                walletPassService.isGoogleWalletEnabled(),
                                walletPassService.isAppleWalletEnabled(),
                                walletPassService.isGenericWalletEnabled()))
                .build();
    }

    @GET
    @Path("/reservations/{id}/{provider}")
    @Produces({
        MediaType.APPLICATION_JSON,
        "application/vnd.apple.pkpass",
        "application/vnd.apple.pkpasses"
    })
    @RolesAllowed({Roles.USER})
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = WalletPassResponseDTO.class)))
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "403", description = "Forbidden")
    @APIResponse(responseCode = "404", description = "Reservation not found")
    public Response getWalletPass(
            @PathParam("id") UUID id, @PathParam("provider") String providerStr) {
        WalletProvider provider = WalletProvider.fromString(providerStr);
        if (!walletPassService.isProviderEnabled(provider)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Wallet provider is disabled")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        User currentUser = userSecurityContext.getCurrentUser();
        LOG.infof(
                "Received GET request for wallet pass (%s) for reservation ID: %s by user: %s (ID:"
                        + " %s)",
                providerStr, id, currentUser.getUsername(), currentUser.id);

        WalletPassResponseDTO responseDTO =
                walletPassService.generatePass(id, currentUser, provider);

        if ((provider == WalletProvider.APPLE || provider == WalletProvider.GENERIC_PKPASS)
                && responseDTO.content() != null) {
            String type =
                    responseDTO.contentType() != null
                            ? responseDTO.contentType()
                            : "application/vnd.apple.pkpass";
            return Response.ok(responseDTO.content())
                    .type(type)
                    .header(
                            "Content-Disposition",
                            "attachment; filename=\"" + responseDTO.filename() + "\"")
                    .build();
        }

        return Response.ok(responseDTO).build();
    }
}
