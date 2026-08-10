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
package de.felixhertweck.seatreservation.email.resource;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.email.service.EmailSeatMapService;
import de.felixhertweck.seatreservation.model.entity.EmailSeatMapToken;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.EmailSeatMapTokenRepository;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import de.felixhertweck.seatreservation.wallet.service.WalletPassService;
import org.jboss.logging.Logger;

@Path("/api/email")
public class EmailSeatMapResource {

    private static final Logger LOG = Logger.getLogger(EmailSeatMapResource.class);

    @Inject EmailSeatMapService service;
    @Inject EmailSeatMapTokenRepository tokenRepository;
    @Inject ReservationRepository reservationRepository;
    @Inject WalletPassService walletPassService;

    @GET
    @Path("/seatmap")
    @Produces("image/svg+xml")
    @PermitAll
    public Response getSeatMap(@QueryParam("token") String token) {
        Optional<String> svg = service.getSvgImage(token);
        if (svg.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Not found or token invalid/expired")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        return Response.ok(svg.get()).type("image/svg+xml").build();
    }

    @GET
    @Path("/wallet/apple")
    @Produces("application/vnd.apple.pkpass")
    @PermitAll
    public Response getAppleWalletPass(@QueryParam("token") String token) {
        return processWalletPass(
                token,
                WalletProvider.APPLE,
                walletPassService.isAppleWalletEnabled(),
                dto ->
                        dto.content() != null
                                ? Response.ok(dto.content())
                                        .type("application/vnd.apple.pkpass")
                                        .header(
                                                "Content-Disposition",
                                                "attachment; filename=\"" + dto.filename() + "\"")
                                        .build()
                                : null);
    }

    @GET
    @Path("/wallet/google")
    @PermitAll
    public Response getGoogleWalletPass(@QueryParam("token") String token) {
        return processWalletPass(
                token,
                WalletProvider.GOOGLE,
                walletPassService.isGoogleWalletEnabled(),
                dto -> dto.url() != null ? Response.seeOther(URI.create(dto.url())).build() : null);
    }

    private Response processWalletPass(
            String token,
            WalletProvider provider,
            boolean enabled,
            java.util.function.Function<WalletPassResponseDTO, Response> successHandler) {
        if (!enabled) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(provider + " Wallet is disabled")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        if (token == null || token.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Token parameter is required")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        Optional<EmailSeatMapToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Invalid token")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        EmailSeatMapToken emailToken = tokenOpt.get();
        if (emailToken.getExpirationTime() != null
                && emailToken.getExpirationTime().isBefore(Instant.now())) {
            return Response.status(Response.Status.GONE)
                    .entity("Token expired")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        User user = emailToken.getUser();
        Event event = emailToken.getEvent();
        List<Reservation> reservations = reservationRepository.findByUserAndEvent(user, event);
        if (reservations.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No reservations found")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        try {
            WalletPassResponseDTO responseDTO =
                    walletPassService.generatePass(reservations.getFirst().id, user, provider);
            Response successResponse = successHandler.apply(responseDTO);
            if (successResponse != null) {
                return successResponse;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate %s pass for email token %s", provider, token);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error generating pass")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
