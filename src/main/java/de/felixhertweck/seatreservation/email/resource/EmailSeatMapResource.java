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

import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.felixhertweck.seatreservation.email.service.EmailSeatMapService;

@Path("/api/email/seatmap")
public class EmailSeatMapResource {

    @Inject EmailSeatMapService service;

    @GET
    @Produces("image/svg+xml")
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
}
