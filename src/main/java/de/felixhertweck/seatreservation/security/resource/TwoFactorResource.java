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

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.dto.TwoFactorBackupCodesDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorDisableDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorEnableDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorRegenerateBackupCodesDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSettingsUpdateDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSetupDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorStatusDTO;
import de.felixhertweck.seatreservation.security.exceptions.InvalidTwoFactorCodeException;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/users/me/2fa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Two-Factor Authentication", description = "Endpoints for managing 2FA settings")
public class TwoFactorResource {

    @Inject UserSecurityContext userSecurityContext;
    @Inject TwoFactorService twoFactorService;

    private User getCurrentUser() {
        return userSecurityContext.getCurrentUser();
    }

    @GET
    @Operation(summary = "Get 2FA status for current user")
    public TwoFactorStatusDTO getStatus() {
        User user = getCurrentUser();
        return twoFactorService.getStatus(user);
    }

    @POST
    @Path("/setup-totp")
    @Operation(summary = "Initiates TOTP setup, returning secret, QR code, and backup codes")
    public TwoFactorSetupDTO setupTotp() {
        User user = getCurrentUser();
        return twoFactorService.setupTotp(user);
    }

    @POST
    @Path("/send-setup-email")
    @Operation(summary = "Sends verification code to email for 2FA setup")
    public Response sendSetupEmail() {
        User user = getCurrentUser();
        twoFactorService.sendSetupEmailCode(user);
        return Response.ok().build();
    }

    @POST
    @Path("/enable")
    @Operation(
            summary =
                    "Enables the given 2FA factor (TOTP or EMAIL) in addition to whatever is"
                            + " already active. TOTP requires a valid code proving possession of"
                            + " the provisioned secret. EMAIL requires no code but the account"
                            + " email must already be verified (403 EmailNotVerifiedException"
                            + " otherwise) -- possession was already proven during account email"
                            + " verification.")
    @APIResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TwoFactorStatusDTO.class)))
    @APIResponse(
            responseCode = "403",
            description = "Email method requested but the account email is not verified")
    @APIResponse(
            responseCode = "429",
            description = "Too many failed TOTP codes submitted recently; account locked")
    public Response enableTwoFactor(@Valid TwoFactorEnableDTO enableDTO) {
        User user = getCurrentUser();
        TwoFactorStatusDTO status =
                twoFactorService
                        .enableTwoFactor(user, enableDTO.method(), enableDTO.code())
                        .orElseThrow(() -> new InvalidTwoFactorCodeException("Invalid 2FA code"));
        return Response.ok(status).build();
    }

    @PUT
    @Path("/settings")
    @Operation(summary = "Updates 2FA settings (passkey requirement)")
    @APIResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TwoFactorStatusDTO.class)))
    public Response updateSettings(@Valid TwoFactorSettingsUpdateDTO updateDTO) {
        User user = getCurrentUser();
        twoFactorService.updateSettings(user, updateDTO.twoFactorPasskeyEnabled());
        return Response.ok(twoFactorService.getStatus(user)).build();
    }

    @POST
    @Path("/disable")
    @Operation(
            summary =
                    "Disables the given 2FA factor (TOTP or EMAIL) for the current user, after"
                            + " verifying a current TOTP/email or backup code. If the other"
                            + " factor is still active, 2FA as a whole stays enabled.")
    @APIResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TwoFactorStatusDTO.class)))
    public Response disableTwoFactor(@Valid TwoFactorDisableDTO disableDTO) {
        User user = getCurrentUser();
        boolean success =
                twoFactorService.disableTwoFactor(user, disableDTO.method(), disableDTO.code());
        if (!success) {
            throw new InvalidTwoFactorCodeException("Invalid code");
        }
        return Response.ok(twoFactorService.getStatus(user)).build();
    }

    @POST
    @Path("/backup-codes")
    @Operation(
            summary =
                    "Regenerates backup codes for the current user, after verifying a current"
                            + " TOTP/email or backup code.")
    @APIResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TwoFactorBackupCodesDTO.class)))
    public TwoFactorBackupCodesDTO regenerateBackupCodes(
            @Valid TwoFactorRegenerateBackupCodesDTO regenerateDTO) {
        User user = getCurrentUser();
        return twoFactorService.regenerateBackupCodes(user, regenerateDTO.code());
    }
}
