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

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HttpForwardFilterTest {

    @InjectMocks private HttpForwardFilter filter;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain chain;

    @Mock private RequestDispatcher dispatcher;

    @Mock private ServletOutputStream servletOutputStream;

    @Test
    void doFilter_ForwardToRootPath() throws Exception {
        when(response.getStatus()).thenReturn(404);
        when(request.getRequestURI()).thenReturn("/some-path");
        when(request.getRequestDispatcher("/")).thenReturn(dispatcher);
        when(response.getOutputStream()).thenReturn(servletOutputStream);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(200);
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_NoForwardForApiOrQuarkusPath() throws Exception {
        when(response.getStatus()).thenReturn(404);
        when(request.getRequestURI()).thenReturn("/api/some-api-path");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(dispatcher, never()).forward(request, response);

        reset(chain);
        when(request.getRequestURI()).thenReturn("/q/some-quarkus-path");
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
        verify(dispatcher, never()).forward(request, response);
    }

    @Test
    void doFilter_NoForwardForNon404Status() throws Exception {
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(dispatcher, never()).forward(request, response);
    }
}
