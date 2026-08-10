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
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.service.EmailSeatMapService;
import de.felixhertweck.seatreservation.wallet.service.WalletPassService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailSeatMapResourceTest {

    @Mock private EmailSeatMapService service;
    @Mock private WalletPassService walletPassService;

    @InjectMocks private EmailSeatMapResource emailSeatMapResource;

    @Test
    void getSeatMap_Success_WithValidToken() {
        String token = "valid-token-123";
        String svgContent = "<svg><rect x=\"0\" y=\"0\" width=\"100\" height=\"100\"/></svg>";

        when(service.getSvgImage(token)).thenReturn(Optional.of(svgContent));

        Response response = emailSeatMapResource.getSeatMap(token);

        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("image/svg+xml", response.getMediaType().toString());
        assertEquals(svgContent, response.getEntity());
    }

    @Test
    void getSeatMap_NotFound_WhenTokenInvalid() {
        String token = "invalid-token";

        when(service.getSvgImage(token)).thenReturn(Optional.empty());

        Response response = emailSeatMapResource.getSeatMap(token);

        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
        assertEquals("Not found or token invalid/expired", response.getEntity());
    }

    @Test
    void getSeatMap_NotFound_WhenTokenExpired() {
        String token = "expired-token";

        when(service.getSvgImage(token)).thenReturn(Optional.empty());

        Response response = emailSeatMapResource.getSeatMap(token);

        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void getSeatMap_NotFound_WhenTokenMissing() {
        when(service.getSvgImage(null)).thenReturn(Optional.empty());

        Response response = emailSeatMapResource.getSeatMap(null);

        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void getSeatMap_NotFound_WhenTokenEmpty() {
        when(service.getSvgImage("")).thenReturn(Optional.empty());

        Response response = emailSeatMapResource.getSeatMap("");

        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void getSeatMap_Success_WithComplexSvg() {
        String token = "valid-token-complex";
        String svgContent =
                "<svg width=\"500\" height=\"400\">"
                        + "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>"
                        + "<rect x=\"100\" y=\"100\" width=\"50\" height=\"50\" fill=\"blue\"/>"
                        + "</svg>";

        when(service.getSvgImage(token)).thenReturn(Optional.of(svgContent));

        Response response = emailSeatMapResource.getSeatMap(token);

        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("image/svg+xml", response.getMediaType().toString());
        String entity = (String) response.getEntity();
        assertTrue(entity.contains("<svg"));
        assertTrue(entity.contains("circle"));
        assertTrue(entity.contains("rect"));
    }

    @Test
    void getAppleWalletPass_BadRequest_WhenTokenMissing() {
        when(walletPassService.isAppleWalletEnabled()).thenReturn(true);
        Response response = emailSeatMapResource.getAppleWalletPass(null);
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void getGoogleWalletPass_BadRequest_WhenTokenMissing() {
        when(walletPassService.isGoogleWalletEnabled()).thenReturn(true);
        Response response = emailSeatMapResource.getGoogleWalletPass(null);
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void getAppleWalletPass_Forbidden_WhenDisabled() {
        when(walletPassService.isAppleWalletEnabled()).thenReturn(false);
        Response response = emailSeatMapResource.getAppleWalletPass("valid-token");
        assertNotNull(response);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void getGoogleWalletPass_Forbidden_WhenDisabled() {
        when(walletPassService.isGoogleWalletEnabled()).thenReturn(false);
        Response response = emailSeatMapResource.getGoogleWalletPass("valid-token");
        assertNotNull(response);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
}
