/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2026 Felix Hertweck
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

import static de.felixhertweck.seatreservation.testutil.TestIds.id;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.TwoFactorDisableDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorEnableDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorRegenerateBackupCodesDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSettingsUpdateDTO;
import de.felixhertweck.seatreservation.security.service.TwoFactorService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TwoFactorResourceTest {

    @Inject UserRepository userRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        User testUser = userRepository.findById(id(1));
        if (testUser != null) {
            testUser.setTwoFactorEnabled(false);
            testUser.setTotpEnabled(false);
            testUser.setEmailEnabled(false);
            testUser.setTotpSecret(null);
            // Some tests flip this off; reset so it doesn't leak into unrelated tests.
            testUser.setEmailVerified(true);
            userRepository.persist(testUser);
        }
    }

    @Test
    public void getStatus_Unauthorized() {
        given().when().get("/api/users/me/2fa").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void getStatus_Success() {
        given().when()
                .get("/api/users/me/2fa")
                .then()
                .statusCode(200)
                .body("twoFactorEnabled", equalTo(false))
                .body("totpEnabled", equalTo(false))
                .body("emailEnabled", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void setupTotp_Success() {
        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/users/me/2fa/setup-totp")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void sendSetupEmail_Success() {
        given().contentType(MediaType.APPLICATION_JSON)
                .when()
                .post("/api/users/me/2fa/send-setup-email")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void enableTwoFactor_InvalidCode_BadRequest() {
        TwoFactorEnableDTO dto = new TwoFactorEnableDTO(TwoFactorMethod.TOTP, "000000");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/enable")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void enableTwoFactor_Success() throws Exception {
        String secret =
                given().contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/users/me/2fa/setup-totp")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("secret");

        byte[] secretBytes = TwoFactorService.decodeBase32(secret);
        com.eatthepath.otp.TimeBasedOneTimePasswordGenerator totp =
                new com.eatthepath.otp.TimeBasedOneTimePasswordGenerator();
        javax.crypto.SecretKey key =
                new javax.crypto.spec.SecretKeySpec(secretBytes, totp.getAlgorithm());
        String code = totp.generateOneTimePasswordString(key, java.time.Instant.now());

        TwoFactorEnableDTO dto = new TwoFactorEnableDTO(TwoFactorMethod.TOTP, code);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/enable")
                .then()
                .statusCode(200)
                .body("twoFactorEnabled", equalTo(true))
                .body("totpEnabled", equalTo(true))
                .body("emailEnabled", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void updateSettings_Success() {
        TwoFactorSettingsUpdateDTO dto = new TwoFactorSettingsUpdateDTO(true);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .put("/api/users/me/2fa/settings")
                .then()
                .statusCode(200)
                // No factor is settable via /settings; both stay off (per setUp) regardless of
                // the passkey toggle below.
                .body("totpEnabled", equalTo(false))
                .body("emailEnabled", equalTo(false))
                .body("twoFactorPasskeyEnabled", equalTo(true));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void disableTwoFactor_NotEnabled_IsIdempotentNoOp() {
        TwoFactorDisableDTO dto = new TwoFactorDisableDTO(TwoFactorMethod.TOTP, "000000");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/disable")
                .then()
                .statusCode(200)
                .body("twoFactorEnabled", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void disableTwoFactor_Enabled_WrongCode_BadRequest() throws Exception {
        enableTotpForCurrentUser();

        TwoFactorDisableDTO dto = new TwoFactorDisableDTO(TwoFactorMethod.TOTP, "000000");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/disable")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void disableTwoFactor_Enabled_ValidCode_Success() throws Exception {
        long enableTotpStep = currentTotpStep();
        String secret = enableTotpForCurrentUser();

        // Replay protection rejects reusing a code from an already-accepted TOTP step (RFC 6238),
        // so the code used to disable must land on a step later than the one /2fa/enable consumed.
        waitForTotpStepAfter(enableTotpStep);

        byte[] secretBytes = TwoFactorService.decodeBase32(secret);
        com.eatthepath.otp.TimeBasedOneTimePasswordGenerator totp =
                new com.eatthepath.otp.TimeBasedOneTimePasswordGenerator();
        javax.crypto.SecretKey key =
                new javax.crypto.spec.SecretKeySpec(secretBytes, totp.getAlgorithm());
        String code = totp.generateOneTimePasswordString(key, java.time.Instant.now());

        TwoFactorDisableDTO dto = new TwoFactorDisableDTO(TwoFactorMethod.TOTP, code);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/disable")
                .then()
                .statusCode(200)
                .body("twoFactorEnabled", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void enableTwoFactor_Email_VerifiedAccount_EnablesInstantlyWithBackupCodes() {
        // The seeded admin user already has a verified email (import-test.sql), so no setup code
        // is needed at all -- possession was already proven via account email verification.
        TwoFactorEnableDTO dto = new TwoFactorEnableDTO(TwoFactorMethod.EMAIL, null);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/enable")
                .then()
                .statusCode(200)
                .body("twoFactorEnabled", equalTo(true))
                .body("emailEnabled", equalTo(true))
                .body("backupCodes.size()", equalTo(8));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void enableTwoFactor_Email_UnverifiedAccount_Forbidden() {
        setEmailVerified(false);

        TwoFactorEnableDTO dto = new TwoFactorEnableDTO(TwoFactorMethod.EMAIL, null);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/enable")
                .then()
                .statusCode(403);
    }

    @Transactional
    void setEmailVerified(boolean verified) {
        User testUser = userRepository.findById(id(1));
        testUser.setEmailVerified(verified);
        userRepository.persist(testUser);
    }

    /** Sets up and enables TOTP for the current test user, returning the base32 secret. */
    private String enableTotpForCurrentUser() throws Exception {
        String secret =
                given().contentType(MediaType.APPLICATION_JSON)
                        .when()
                        .post("/api/users/me/2fa/setup-totp")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("secret");

        byte[] secretBytes = TwoFactorService.decodeBase32(secret);
        com.eatthepath.otp.TimeBasedOneTimePasswordGenerator totp =
                new com.eatthepath.otp.TimeBasedOneTimePasswordGenerator();
        javax.crypto.SecretKey key =
                new javax.crypto.spec.SecretKeySpec(secretBytes, totp.getAlgorithm());
        String code = totp.generateOneTimePasswordString(key, java.time.Instant.now());

        TwoFactorEnableDTO dto = new TwoFactorEnableDTO(TwoFactorMethod.TOTP, code);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/enable")
                .then()
                .statusCode(200);

        return secret;
    }

    private static long currentTotpStep() {
        return java.time.Instant.now().getEpochSecond() / 30;
    }

    /** Blocks until the current 30s TOTP step is strictly later than {@code step}. */
    private static void waitForTotpStepAfter(long step) throws InterruptedException {
        while (currentTotpStep() <= step) {
            Thread.sleep(500);
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void regenerateBackupCodes_ValidCode_Success() throws Exception {
        long enableTotpStep = currentTotpStep();
        String secret = enableTotpForCurrentUser();

        // Replay protection rejects reusing a code from an already-accepted TOTP step (RFC 6238),
        // so the code used to regenerate must land on a step later than the one /2fa/enable
        // consumed.
        waitForTotpStepAfter(enableTotpStep);

        byte[] secretBytes = TwoFactorService.decodeBase32(secret);
        com.eatthepath.otp.TimeBasedOneTimePasswordGenerator totp =
                new com.eatthepath.otp.TimeBasedOneTimePasswordGenerator();
        javax.crypto.SecretKey key =
                new javax.crypto.spec.SecretKeySpec(secretBytes, totp.getAlgorithm());
        String code = totp.generateOneTimePasswordString(key, java.time.Instant.now());

        TwoFactorRegenerateBackupCodesDTO dto = new TwoFactorRegenerateBackupCodesDTO(code);

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/backup-codes")
                .then()
                .statusCode(200)
                .body("backupCodes.size()", equalTo(8));
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void regenerateBackupCodes_InvalidCode_BadRequest() throws Exception {
        enableTotpForCurrentUser();

        TwoFactorRegenerateBackupCodesDTO dto = new TwoFactorRegenerateBackupCodesDTO("000000");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/backup-codes")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void regenerateBackupCodes_NoTwoFactorEnabled_BadRequest() {
        TwoFactorRegenerateBackupCodesDTO dto = new TwoFactorRegenerateBackupCodesDTO("000000");

        given().contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .when()
                .post("/api/users/me/2fa/backup-codes")
                .then()
                .statusCode(400);
    }
}
