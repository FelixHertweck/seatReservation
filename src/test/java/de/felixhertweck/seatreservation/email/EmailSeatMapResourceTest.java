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
package de.felixhertweck.seatreservation.email;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.service.EmailSeatMapService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EmailSeatMapResourceTest {

    @InjectMock EmailSeatMapService emailSeatMapService;

    @Test
    void getSeatMap_Success_WithValidToken() {
        String token = "valid-token-123";
        String svgContent = "<svg><rect x=\"0\" y=\"0\" width=\"100\" height=\"100\"/></svg>";

        when(emailSeatMapService.getSvgImage(token)).thenReturn(Optional.of(svgContent));

        given().queryParam("token", token)
                .when()
                .get("/api/email/seatmap")
                .then()
                .statusCode(200)
                .contentType("image/svg+xml")
                .body(containsString("<svg"));
    }

    @Test
    void getSeatMap_NotFound_WhenTokenInvalid() {
        String token = "invalid-token";

        when(emailSeatMapService.getSvgImage(token)).thenReturn(Optional.empty());

        given().queryParam("token", token)
                .when()
                .get("/api/email/seatmap")
                .then()
                .statusCode(404)
                .contentType("text/plain")
                .body(containsString("Not found or token invalid/expired"));
    }

    @Test
    void getSeatMap_NotFound_WhenTokenExpired() {
        String token = "expired-token";

        when(emailSeatMapService.getSvgImage(token)).thenReturn(Optional.empty());

        given().queryParam("token", token).when().get("/api/email/seatmap").then().statusCode(404);
    }

    @Test
    void getSeatMap_NotFound_WhenTokenMissing() {
        when(emailSeatMapService.getSvgImage(anyString())).thenReturn(Optional.empty());

        given().when().get("/api/email/seatmap").then().statusCode(404);
    }

    @Test
    void getSeatMap_NotFound_WhenTokenEmpty() {
        when(emailSeatMapService.getSvgImage("")).thenReturn(Optional.empty());

        given().queryParam("token", "").when().get("/api/email/seatmap").then().statusCode(404);
    }

    @Test
    void getSeatMap_Success_WithComplexSvg() {
        String token = "valid-token-complex";
        String svgContent =
                "<svg width=\"500\" height=\"400\">"
                        + "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"green\"/>"
                        + "<rect x=\"100\" y=\"100\" width=\"50\" height=\"50\" fill=\"blue\"/>"
                        + "</svg>";

        when(emailSeatMapService.getSvgImage(token)).thenReturn(Optional.of(svgContent));

        given().queryParam("token", token)
                .when()
                .get("/api/email/seatmap")
                .then()
                .statusCode(200)
                .contentType("image/svg+xml")
                .body(containsString("<svg"))
                .body(containsString("circle"))
                .body(containsString("rect"));
    }
}
