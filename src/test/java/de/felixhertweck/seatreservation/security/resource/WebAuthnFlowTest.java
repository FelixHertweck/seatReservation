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
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import de.felixhertweck.seatreservation.security.dto.TwoFactorEnableDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSettingsUpdateDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorVerifyRequestDTO;
import de.felixhertweck.seatreservation.security.dto.WebAuthnRegistrationStartDTO;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
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

    private static final String CHROME_ON_WINDOWS_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                    + " Chrome/125.0.0.0 Safari/537.36";

    private static String extractChallenge(String optionsJson) {
        return new JsonObject(optionsJson).getString("challenge");
    }

    @Test
    void fullPasskeyLifecycle() {
        WebAuthnHardware authenticator = new WebAuthnHardware(url);
        String username = "passkey_lifecycle";
        CookieFilter registrationCookies = new CookieFilter();
        JsonObject registrationDetails =
                new JsonObject()
                        .put("username", username)
                        .put("firstname", "Ada")
                        .put("lastname", "Lovelace")
                        .put("email", username + "@example.com");

        // 1. Obtain creation options for a brand-new passkey-only account.
        String registerOptions =
                given().filter(registrationCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(registrationDetails.encode())
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
                        .put("registration", registrationDetails)
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
                .header("User-Agent", CHROME_ON_WINDOWS_UA)
                .contentType(MediaType.APPLICATION_JSON)
                .body(secondAttestation.encode())
                .when()
                .post("/api/auth/webauthn/register")
                .then()
                .statusCode(200);

        // 6. Both passkeys are listed; the second got a sensible default name from the User-Agent.
        List<String> ids =
                given().filter(loginCookies)
                        .when()
                        .get("/api/auth/webauthn/credentials")
                        .then()
                        .statusCode(200)
                        .body("size()", equalTo(2))
                        .body("label", hasItem("Chrome on Windows"))
                        .extract()
                        .jsonPath()
                        .getList("id");

        // 6b. A passkey can be renamed, and the new label is reflected in the listing.
        given().filter(loginCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JsonObject().put("label", "My work laptop").encode())
                .when()
                .put("/api/auth/webauthn/credentials/" + ids.get(0))
                .then()
                .statusCode(200);
        given().filter(loginCookies)
                .when()
                .get("/api/auth/webauthn/credentials")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + ids.get(0) + "' }.label", equalTo("My work laptop"));

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
    void passkeyLogin_TwoFactorGateEnabled_RequiresChallengeThenVerifies() throws Exception {
        WebAuthnHardware authenticator = new WebAuthnHardware(url);
        String username = "passkey_2fa_user";
        CookieFilter registrationCookies = new CookieFilter();
        JsonObject registrationDetails =
                new JsonObject()
                        .put("username", username)
                        .put("firstname", "Grace")
                        .put("lastname", "Hopper")
                        .put("email", username + "@example.com");

        // 1. Create a passkey-only account (also logs the session in via cookies).
        String registerOptions =
                given().filter(registrationCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(registrationDetails.encode())
                        .when()
                        .post("/api/auth/webauthn/register-new/options")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();
        JsonObject attestation =
                authenticator.makeRegistrationJson(extractChallenge(registerOptions));
        JsonObject registerBody =
                new JsonObject()
                        .put("registration", registrationDetails)
                        .put("credential", attestation);
        given().filter(registrationCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerBody.encode())
                .when()
                .post("/api/auth/webauthn/register-new")
                .then()
                .statusCode(200);

        // 2. Enable TOTP 2FA and require it after passkey login too.
        String secret =
                given().filter(registrationCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/users/me/2fa/setup-totp")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("secret");
        long enableTotpStep = currentTotpStep();
        String totpCode = generateTotpCode(secret);
        given().filter(registrationCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TwoFactorEnableDTO(TwoFactorMethod.TOTP, totpCode))
                .when()
                .post("/api/users/me/2fa/enable")
                .then()
                .statusCode(200);
        given().filter(registrationCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TwoFactorSettingsUpdateDTO(true))
                .when()
                .put("/api/users/me/2fa/settings")
                .then()
                .statusCode(200)
                .body("twoFactorPasskeyEnabled", equalTo(true));

        // 3. A fresh passkey login is now gated behind 2FA: no auth cookie, a challenge instead.
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
        io.restassured.response.Response loginResponse =
                given().filter(loginCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(assertion.encode())
                        .when()
                        .post("/api/auth/webauthn/login");
        loginResponse
                .then()
                .statusCode(200)
                .body("twoFactorRequired", equalTo(true))
                .body("totpAvailable", equalTo(true))
                .body("emailAvailable", equalTo(false));
        org.junit.jupiter.api.Assertions.assertTrue(
                loginResponse.getHeaders().getValues("Set-Cookie").stream()
                        .noneMatch(header -> header.startsWith("jwt=")),
                "No JWT cookie should be set before the 2FA challenge is verified");
        String challengeToken = loginResponse.jsonPath().getString("challengeToken");

        // 4. A wrong code is rejected...
        given().filter(loginCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TwoFactorVerifyRequestDTO(challengeToken, "000000"))
                .when()
                .post("/api/auth/2fa/verify")
                .then()
                .statusCode(401);

        // Replay protection rejects reusing a code from an already-accepted TOTP step (RFC 6238),
        // so make sure we're generating a code for a step later than the one /2fa/enable consumed
        // above before logging in with it.
        waitForTotpStepAfter(enableTotpStep);

        // 5. ...but the correct TOTP code completes the login with auth cookies.
        given().filter(loginCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TwoFactorVerifyRequestDTO(challengeToken, generateTotpCode(secret)))
                .when()
                .post("/api/auth/2fa/verify")
                .then()
                .statusCode(200)
                .cookie("jwt", notNullValue());
    }

    private static String generateTotpCode(String base32Secret) throws Exception {
        byte[] secretBytes = TwoFactorService.decodeBase32(base32Secret);
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        SecretKey key = new SecretKeySpec(secretBytes, totp.getAlgorithm());
        return totp.generateOneTimePasswordString(key, Instant.now());
    }

    private static long currentTotpStep() {
        return Instant.now().getEpochSecond() / 30;
    }

    /** Blocks until the current 30s TOTP step is strictly later than {@code step}. */
    private static void waitForTotpStepAfter(long step) throws InterruptedException {
        while (currentTotpStep() <= step) {
            Thread.sleep(500);
        }
    }

    @Test
    void registerNewOptions_duplicateUsername_conflict() {
        // 'admin' is seeded in import-test.sql.
        given().contentType(MediaType.APPLICATION_JSON)
                .body(
                        new JsonObject()
                                .put("username", "admin")
                                .put("firstname", "Ada")
                                .put("lastname", "Lovelace")
                                .put("email", "admin-duplicate@example.com")
                                .encode())
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

    @Test
    void defaultDeviceLabel_derivesBrowserAndOs() {
        org.junit.jupiter.api.Assertions.assertEquals(
                "Chrome on Windows", WebAuthnResource.defaultDeviceLabel(CHROME_ON_WINDOWS_UA));
        org.junit.jupiter.api.Assertions.assertEquals(
                "Safari on iPhone",
                WebAuthnResource.defaultDeviceLabel(
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"
                            + " AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148"
                            + " Safari/604.1"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "Firefox on Linux",
                WebAuthnResource.defaultDeviceLabel(
                        "Mozilla/5.0 (X11; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0"));
        // Edge must win over the "Chrome" token it also carries.
        org.junit.jupiter.api.Assertions.assertEquals(
                "Edge on Windows",
                WebAuthnResource.defaultDeviceLabel(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like"
                                + " Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0"));
        // Unrecognisable agents fall back to null so the client shows a generic label.
        org.junit.jupiter.api.Assertions.assertNull(WebAuthnResource.defaultDeviceLabel(null));
        org.junit.jupiter.api.Assertions.assertNull(WebAuthnResource.defaultDeviceLabel("   "));
    }
}
