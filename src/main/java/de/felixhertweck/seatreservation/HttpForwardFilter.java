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

@WebFilter("/*")
public class HttpForwardFilter extends HttpFilter {
    public static final String API_ROOT_PATH = "/api";
    public static final String QUARKUS_ROOT_PATH = "/q";

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
