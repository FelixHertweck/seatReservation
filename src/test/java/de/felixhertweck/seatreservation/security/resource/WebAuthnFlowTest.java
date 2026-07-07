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

import java.net.URL;
import java.util.List;
import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import de.felixhertweck.seatreservation.security.dto.WebAuthnRegistrationStartDTO;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.filter.cookie.CookieFilter;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * End-to-end passkey ceremony tests driven by a simulated authenticator ({@link WebAuthnHardware}).
 * Exercises the full journey: create a passkey-only account, log in with the passkey, add a second
 * passkey, and manage/delete credentials.
 */
@QuarkusTest
class WebAuthnFlowTest {

    @TestHTTPResource URL url;

    private static String extractChallenge(String optionsJson) {
        return new JsonObject(optionsJson).getString("challenge");
    }

    @Test
    void fullPasskeyLifecycle() {
        WebAuthnHardware authenticator = new WebAuthnHardware(url);
        String username = "passkey_lifecycle";
        CookieFilter registrationCookies = new CookieFilter();

        // 1. Obtain creation options for a brand-new passkey-only account.
        String registerOptions =
                given().filter(registrationCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new JsonObject().put("username", username).encode())
                        .when()
                        .post("/api/auth/webauthn/register-new/options")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();
        JsonObject attestation =
                authenticator.makeRegistrationJson(extractChallenge(registerOptions));

        // 2. Complete registration: creates the account and logs in (JWT cookie set).
        JsonObject registerBody =
                new JsonObject()
                        .put("registration", new JsonObject().put("username", username))
                        .put("credential", attestation);
        given().filter(registrationCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerBody.encode())
                .when()
                .post("/api/auth/webauthn/register-new")
                .then()
                .statusCode(200)
                .cookie("jwt", notNullValue())
                .cookie("refreshToken", notNullValue());

        // 3. Log in from a fresh session using the same passkey.
        CookieFilter loginCookies = new CookieFilter();
        String loginOptions =
                given().filter(loginCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("username", username)
                        .when()
                        .post("/api/auth/webauthn/login/options")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();
        JsonObject assertion = authenticator.makeLoginJson(extractChallenge(loginOptions));
        given().filter(loginCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(assertion.encode())
                .when()
                .post("/api/auth/webauthn/login")
                .then()
                .statusCode(200)
                .cookie("jwt", notNullValue());

        // 4. Status reflects a passkey-only account.
        given().filter(loginCookies)
                .when()
                .get("/api/auth/webauthn/status")
                .then()
                .statusCode(200)
                .body("hasPasskey", equalTo(true))
                .body("hasPassword", equalTo(false));

        // 5. Add a second passkey while authenticated.
        WebAuthnHardware secondAuthenticator = new WebAuthnHardware(url);
        String addOptions =
                given().filter(loginCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/auth/webauthn/register/options")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();
        JsonObject secondAttestation =
                secondAuthenticator.makeRegistrationJson(extractChallenge(addOptions));
        given().filter(loginCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(secondAttestation.encode())
                .when()
                .post("/api/auth/webauthn/register")
                .then()
                .statusCode(200);

        // 6. Both passkeys are listed.
        List<Integer> ids =
                given().filter(loginCookies)
                        .when()
                        .get("/api/auth/webauthn/credentials")
                        .then()
                        .statusCode(200)
                        .body("size()", equalTo(2))
                        .extract()
                        .jsonPath()
                        .getList("id");

        // 7. Deleting one succeeds; deleting the last of a password-less account is refused (409).
        given().filter(loginCookies)
                .when()
                .delete("/api/auth/webauthn/credentials/" + ids.get(0))
                .then()
                .statusCode(204);
        given().filter(loginCookies)
                .when()
                .delete("/api/auth/webauthn/credentials/" + ids.get(1))
                .then()
                .statusCode(409);
    }

    @Test
    void registerNewOptions_duplicateUsername_conflict() {
        // 'admin' is seeded in import-test.sql.
        given().contentType(MediaType.APPLICATION_JSON)
                .body(new JsonObject().put("username", "admin").encode())
                .when()
                .post("/api/auth/webauthn/register-new/options")
                .then()
                .statusCode(409);
    }

    @Test
    void managementEndpointsRequireAuthentication() {
        given().when().get("/api/auth/webauthn/status").then().statusCode(401);
        given().when().get("/api/auth/webauthn/credentials").then().statusCode(401);
        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/auth/webauthn/register/options")
                .then()
                .statusCode(401);
    }

    @Test
    void loginWithUnknownCredential_isRejected() {
        WebAuthnHardware authenticator = new WebAuthnHardware(url);
        CookieFilter cookies = new CookieFilter();
        String loginOptions =
                given().filter(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/auth/webauthn/login/options")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();
        // The authenticator never registered, so no matching credential exists.
        JsonObject assertion = authenticator.makeLoginJson(extractChallenge(loginOptions));
        given().filter(cookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(assertion.encode())
                .when()
                .post("/api/auth/webauthn/login")
                .then()
                .statusCode(401);
    }

    // Ensures the DTO stays referenced from tests for display-name behaviour used by the ceremony.
    @Test
    void displayNameFallsBackToUsername() {
        WebAuthnRegistrationStartDTO dto = new WebAuthnRegistrationStartDTO();
        dto.setUsername("only_username");
        org.junit.jupiter.api.Assertions.assertEquals("only_username", dto.getDisplayName());
        dto.setFirstname("Ada");
        dto.setLastname("Lovelace");
        org.junit.jupiter.api.Assertions.assertEquals("Ada Lovelace", dto.getDisplayName());
    }
}
