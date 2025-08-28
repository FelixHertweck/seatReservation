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
package de.felixhertweck.seatreservation.security.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.security.dto.LoginRequestDTO;
import de.felixhertweck.seatreservation.security.dto.RegisterRequestDTO;
import de.felixhertweck.seatreservation.security.service.AuthService;
import de.felixhertweck.seatreservation.security.service.TokenService;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject AuthService authService;
    @Inject TokenService tokenService;

    @POST
    @Path("/login")
    @APIResponse(responseCode = "200", description = "Login successful, JWT cookie set")
    @APIResponse(responseCode = "401", description = "Unauthorized: Invalid credentials")
    public Response login(@Valid LoginRequestDTO loginRequest) {
        LOG.infof("Received login request for username: %s", loginRequest.getUsername());
        LOG.debugf("LoginRequestDTO: %s", loginRequest.toString());
        String token =
                authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        NewCookie jwtCookie = tokenService.createNewJwtCookie(token);
        LOG.infof("User %s logged in successfully. JWT cookie set.", loginRequest.getUsername());
        return Response.ok().cookie(jwtCookie).build();
    }

    @POST
    @Path("/register")
    @APIResponse(responseCode = "200", description = "Registration successful, JWT cookie set")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: User with this username already exists")
    public Response register(@Valid RegisterRequestDTO registerRequest) {
        LOG.infof("Received registration request for username: %s", registerRequest.getUsername());
        LOG.debugf("RegisterRequestDTO: %s", registerRequest.toString());

        String token = authService.register(registerRequest);

        NewCookie jwtCookie = tokenService.createNewJwtCookie(token);

        LOG.infof("User %s registered successfully.", registerRequest.getUsername());

        return Response.ok().cookie(jwtCookie).build();
    }

    @POST
    @Path("/logout")
    @APIResponse(responseCode = "200", description = "Logout successful, JWT cookie cleared")
    public Response logout() {
        LOG.infof("Received logout request.");
        NewCookie jwtCookie =
                new NewCookie.Builder("jwt")
                        .value("")
                        .path("/")
                        .maxAge(0)
                        .httpOnly(true)
                        .secure(true)
                        .build();
        LOG.infof("User logged out successfully. JWT cookie cleared.");
        return Response.ok().cookie(jwtCookie).build();
    }
}
