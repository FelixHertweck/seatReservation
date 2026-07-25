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

import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSettingsDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSetupDTO;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/user/2fa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TwoFactorResource {

    private static final Logger LOG = Logger.getLogger(TwoFactorResource.class);

    @Inject TwoFactorService twoFactorService;

    @Inject UserSecurityContext userSecurityContext;

    @GET
    @Path("/settings")
    @APIResponse(responseCode = "200", description = "2FA settings retrieved")
    public TwoFactorSettingsDTO getSettings() {
        User user = userSecurityContext.getCurrentUser();
        return twoFactorService.getSettings(user);
    }

    @PUT
    @Path("/settings")
    @APIResponse(responseCode = "200", description = "2FA settings updated")
    public TwoFactorSettingsDTO updateSettings(@Valid TwoFactorSettingsDTO settingsDTO) {
        User user = userSecurityContext.getCurrentUser();
        return twoFactorService.updateSettings(user, settingsDTO);
    }

    @POST
    @Path("/setup/totp")
    @APIResponse(responseCode = "200", description = "TOTP setup initiated")
    public TwoFactorSetupDTO setupTotp() {
        User user = userSecurityContext.getCurrentUser();
        return twoFactorService.setup(user);
    }

    @POST
    @Path("/enable")
    @APIResponse(responseCode = "200", description = "2FA enabled, returns backup codes")
    @APIResponse(responseCode = "400", description = "Invalid code")
    public Response enableTwoFactor(Map<String, String> payload) {
        String code = payload.get("code");
        if (code == null || code.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Code is required").build();
        }
        User user = userSecurityContext.getCurrentUser();
        List<String> backupCodes = twoFactorService.enable(user, code);
        LOG.infof("User ID: %d enabled 2FA.", user.id);
        return Response.ok(backupCodes).build();
    }

    @DELETE
    @Path("/disable")
    @APIResponse(responseCode = "204", description = "2FA disabled")
    public Response disableTwoFactor() {
        User user = userSecurityContext.getCurrentUser();
        twoFactorService.disable(user);
        LOG.infof("User ID: %d disabled 2FA.", user.id);
        return Response.noContent().build();
    }
}
