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
package de.felixhertweck.seatreservation;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

/**
 * HTTP servlet filter for handling client-side routing and forwarding. Redirects non-API and
 * non-Quarkus admin requests to the root path (/) for single-page application (SPA) routing.
 * Returns 404 requests for non-API paths to the root path to support client-side routing.
 */
@WebFilter("/*")
public class HttpForwardFilter extends HttpFilter {
    public static final String API_ROOT_PATH = "/api";
    public static final String QUARKUS_ROOT_PATH = "/q";

    /**
     * Filters HTTP requests to handle 404 responses for SPA routing. Requests to API and Quarkus
     * paths are passed through without modification. Other 404 requests are redirected to the root
     * path for client-side routing.
     *
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @param chain the filter chain for processing the request
     * @throws ServletException if a servlet-related exception occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilter(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (response.getStatus() != 404) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith(API_ROOT_PATH) || path.startsWith(QUARKUS_ROOT_PATH)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            response.reset();
            response.setStatus(200);
            response.setContentType(MediaType.TEXT_HTML);
            request.getRequestDispatcher("/").forward(request, response);
        } finally {
            response.getOutputStream().close();
        }
    }
}
