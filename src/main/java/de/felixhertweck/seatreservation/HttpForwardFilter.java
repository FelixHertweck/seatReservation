package de.felixhertweck.seatreservation;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

public class HttpForwardFilter extends HttpFilter {
    public static final String API_ROOT_PATH = "/api";
    public static final String QUARKUS_ROOT_PATH = "/q";

    @Override
    protected void doFilter(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        super.doFilter(request, response, chain);

        if (response.getStatus() != 404) {
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith(API_ROOT_PATH) || path.startsWith(QUARKUS_ROOT_PATH)) {
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
