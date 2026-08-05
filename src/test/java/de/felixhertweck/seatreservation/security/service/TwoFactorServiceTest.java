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
package de.felixhertweck.seatreservation.security.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.model.entity.TwoFactorBackupCode;
import de.felixhertweck.seatreservation.model.entity.TwoFactorChallenge;
import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.TwoFactorAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.TwoFactorBackupCodeRepository;
import de.felixhertweck.seatreservation.model.repository.TwoFactorChallengeRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.TwoFactorBackupCodesDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorRequiredDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSetupDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorStatusDTO;
import de.felixhertweck.seatreservation.security.exceptions.AccountLockedException;
import de.felixhertweck.seatreservation.security.exceptions.EmailNotVerifiedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class TwoFactorServiceTest {

    @InjectMock UserRepository userRepository;
    @InjectMock TwoFactorBackupCodeRepository backupCodeRepository;
    @InjectMock TwoFactorChallengeRepository challengeRepository;
    @InjectMock TwoFactorAttemptRepository twoFactorAttemptRepository;
    @InjectMock EmailService emailService;
    @InjectMock EmailCooldownService emailCooldownService;

    TwoFactorService twoFactorService;

    @BeforeEach
    void setUp() {
        Mockito.reset(
                userRepository,
                backupCodeRepository,
                challengeRepository,
                twoFactorAttemptRepository,
                emailService,
                emailCooldownService);
        twoFactorService = new TwoFactorService();
        twoFactorService.userRepository = userRepository;
        twoFactorService.backupCodeRepository = backupCodeRepository;
        twoFactorService.challengeRepository = challengeRepository;
        twoFactorService.twoFactorAttemptRepository = twoFactorAttemptRepository;
        twoFactorService.emailService = emailService;
        twoFactorService.emailCooldownService = emailCooldownService;
        twoFactorService.maxFailedAttempts = 5;
        twoFactorService.lockoutDurationSeconds = 300;
        when(emailCooldownService.checkAndRecord(any(), any())).thenReturn(Optional.empty());
    }

    /** Generates a currently-valid TOTP code for the given raw secret bytes. */
    private static String validTotpCodeFor(byte[] secretBytes) throws Exception {
        com.eatthepath.otp.TimeBasedOneTimePasswordGenerator totp =
                new com.eatthepath.otp.TimeBasedOneTimePasswordGenerator();
        javax.crypto.SecretKey key =
                new javax.crypto.spec.SecretKeySpec(secretBytes, totp.getAlgorithm());
        return totp.generateOneTimePasswordString(key, Instant.now());
    }

    private static final byte[] TOTP_SECRET_BYTES =
            new byte[] {0x12, 0x34, 0x56, 0x78, (byte) 0x90, 0x12, 0x34, 0x56, 0x78, 0x12};

    @Test
    void testGetStatus() {
        User user = new User();
        user.setUsername("testuser");
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setTwoFactorPasskeyEnabled(false);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        when(backupCodeRepository.countUnusedByUser(user)).thenReturn(8L);

        TwoFactorStatusDTO status = twoFactorService.getStatus(user);

        assertNotNull(status);
        assertTrue(status.twoFactorEnabled());
        assertTrue(status.totpEnabled());
        assertFalse(status.emailEnabled());
        assertFalse(status.twoFactorPasskeyEnabled());
        assertTrue(status.hasTotpSecret());
        assertEquals(8L, status.remainingBackupCodes());
        assertNull(status.backupCodes());
    }

    @Test
    void testSetupTotp() {
        User user = new User();
        user.setUsername("totpuser");

        TwoFactorSetupDTO dto = twoFactorService.setupTotp(user);

        assertNotNull(dto);
        assertNotNull(dto.secret());
        assertNotNull(dto.otpauthUrl());
        assertTrue(dto.otpauthUrl().contains("totpuser"));
        assertNotNull(dto.qrCodeDataUrl());
        assertTrue(dto.qrCodeDataUrl().startsWith("data:image/png;base64,"));
        assertNotNull(dto.backupCodes());
        assertEquals(8, dto.backupCodes().size());

        verify(userRepository, times(1)).persist(user);
        verify(backupCodeRepository, times(1)).deleteByUser(user);
        verify(backupCodeRepository, times(8)).persist(any(TwoFactorBackupCode.class));
    }

    @Test
    void testSetupTotp_AlreadyEnabled_Throws() {
        User user = new User();
        user.setUsername("totpuser");
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        assertThrows(
                de.felixhertweck.seatreservation.security.exceptions
                        .TwoFactorAlreadyEnabledException.class,
                () -> twoFactorService.setupTotp(user));

        verify(userRepository, never()).persist(user);
        verify(backupCodeRepository, never()).deleteByUser(user);
    }

    @Test
    void testSetupTotp_ExistingUnusedBackupCodes_DoesNotRegenerate() {
        User user = new User();
        user.setUsername("totpuser");
        when(backupCodeRepository.countUnusedByUser(user)).thenReturn(3L);

        TwoFactorSetupDTO dto = twoFactorService.setupTotp(user);

        assertNotNull(dto.secret());
        assertNull(dto.backupCodes());
        verify(backupCodeRepository, never()).deleteByUser(user);
        verify(backupCodeRepository, never()).persist(any(TwoFactorBackupCode.class));
    }

    @Test
    void testSendSetupEmailCode() {
        User user = new User();
        user.setUsername("emailuser");
        user.setEmail("emailuser@example.com");

        twoFactorService.sendSetupEmailCode(user);

        verify(challengeRepository, times(1)).deleteByUser(user);
        verify(challengeRepository, times(1)).persist(any(TwoFactorChallenge.class));
        verify(emailService, times(1)).sendTwoFactorCode(eq(user), any(String.class));
    }

    @Test
    void testEnableTwoFactor_TotpSuccess() throws Exception {
        User user = new User();
        user.setUsername("totpuser");
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);

        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        Optional<TwoFactorStatusDTO> result =
                twoFactorService.enableTwoFactor(user, TwoFactorMethod.TOTP, validCode);

        assertTrue(result.isPresent());
        assertTrue(result.get().twoFactorEnabled());
        assertTrue(result.get().totpEnabled());
        assertFalse(result.get().emailEnabled());
        assertNull(result.get().backupCodes());
        assertTrue(user.isTwoFactorEnabled());
        assertTrue(user.isTotpEnabled());
        verify(userRepository, times(1)).persist(user);
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, true);
    }

    @Test
    void testEnableTwoFactor_TotpInvalidCode_RecordsFailedAttempt() {
        User user = new User();
        user.setUsername("totpuser");
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);

        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of());

        Optional<TwoFactorStatusDTO> result =
                twoFactorService.enableTwoFactor(user, TwoFactorMethod.TOTP, "000000");

        assertTrue(result.isEmpty());
        assertFalse(user.isTotpEnabled());
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, false);
    }

    @Test
    void testEnableTwoFactor_Totp_TooManyFailedAttempts_ThrowsAccountLocked() {
        User user = new User();
        user.setUsername("totpuser");
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);

        when(twoFactorAttemptRepository.countFailedAttempts(eq(user), any(Instant.class)))
                .thenReturn(5L);

        // Guessing codes against a freshly-provisioned-but-not-yet-enabled secret is just as much
        // a brute-force surface as the login challenge or the disable/email-change path, so it
        // must be subject to the same lockout.
        assertThrows(
                AccountLockedException.class,
                () -> twoFactorService.enableTwoFactor(user, TwoFactorMethod.TOTP, "000000"));
    }

    @Test
    void testEnableTwoFactor_TotpNoSecret_ReturnsEmpty() {
        User user = new User();
        user.setUsername("totpuser");
        user.setTotpSecret(null);

        Optional<TwoFactorStatusDTO> result =
                twoFactorService.enableTwoFactor(user, TwoFactorMethod.TOTP, "123456");

        assertTrue(result.isEmpty());
        assertFalse(user.isTwoFactorEnabled());
    }

    @Test
    void testEnableTwoFactor_Email_Verified_FirstActivation_ReturnsFreshBackupCodes() {
        User user = new User();
        user.setUsername("emailuser");
        user.setEmail("emailuser@example.com");
        user.setEmailVerified(true);

        when(backupCodeRepository.countUnusedByUser(user)).thenReturn(0L);

        // No code needed -- possession of the email address was already proven via account email
        // verification, so this must succeed even with a blank/absent code.
        Optional<TwoFactorStatusDTO> result =
                twoFactorService.enableTwoFactor(user, TwoFactorMethod.EMAIL, null);

        assertTrue(result.isPresent());
        assertTrue(result.get().twoFactorEnabled());
        assertTrue(result.get().emailEnabled());
        assertFalse(result.get().totpEnabled());
        assertNotNull(result.get().backupCodes());
        assertEquals(8, result.get().backupCodes().size());
        assertTrue(user.isTwoFactorEnabled());
        assertTrue(user.isEmailEnabled());
        verify(userRepository, times(1)).persist(user);
        verify(backupCodeRepository, times(1)).deleteByUser(user);
    }

    @Test
    void testEnableTwoFactor_Email_Verified_AlreadyHasBackupCodes_DoesNotRegenerate() {
        User user = new User();
        user.setUsername("emailuser");
        user.setEmail("emailuser@example.com");
        user.setEmailVerified(true);

        when(backupCodeRepository.countUnusedByUser(user)).thenReturn(5L);

        Optional<TwoFactorStatusDTO> result =
                twoFactorService.enableTwoFactor(user, TwoFactorMethod.EMAIL, null);

        assertTrue(result.isPresent());
        assertNull(result.get().backupCodes());
        verify(backupCodeRepository, never()).deleteByUser(user);
    }

    @Test
    void testEnableTwoFactor_AddingEmailWhileTotpAlreadyEnabled_BothActive() {
        User user = new User();
        user.setUsername("bothuser");
        user.setEmail("bothuser@example.com");
        user.setEmailVerified(true);
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setTotpSecret(TwoFactorService.encodeBase32(TOTP_SECRET_BYTES));

        when(backupCodeRepository.countUnusedByUser(user)).thenReturn(5L);

        Optional<TwoFactorStatusDTO> result =
                twoFactorService.enableTwoFactor(user, TwoFactorMethod.EMAIL, null);

        assertTrue(result.isPresent());
        assertTrue(result.get().totpEnabled());
        assertTrue(result.get().emailEnabled());
        assertTrue(user.isTotpEnabled());
        assertTrue(user.isEmailEnabled());
    }

    @Test
    void testEnableTwoFactor_Email_NotVerified_ThrowsEmailNotVerifiedException() {
        User user = new User();
        user.setUsername("unverifieduser");
        user.setEmail("unverifieduser@example.com");
        user.setEmailVerified(false);

        assertThrows(
                EmailNotVerifiedException.class,
                () -> twoFactorService.enableTwoFactor(user, TwoFactorMethod.EMAIL, "654321"));

        assertFalse(user.isEmailEnabled());
        assertFalse(user.isTwoFactorEnabled());
        verify(userRepository, never()).persist(user);
    }

    @Test
    void testUpdateSettings_OnlyTogglesPasskey() {
        User user = new User();
        user.setTotpEnabled(true);
        user.setTwoFactorPasskeyEnabled(false);

        twoFactorService.updateSettings(user, true);

        assertTrue(user.isTotpEnabled());
        assertTrue(user.isTwoFactorPasskeyEnabled());
        verify(userRepository, times(1)).persist(user);
    }

    @Test
    void testSaveUser_ExistingUser_UsesMergeNotPersist() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.setTwoFactorPasskeyEnabled(false);

        EntityManager entityManager = Mockito.mock(EntityManager.class);
        when(userRepository.getEntityManager()).thenReturn(entityManager);

        twoFactorService.updateSettings(user, true);

        verify(entityManager, times(1)).merge(user);
        verify(userRepository, never()).persist(user);
    }

    @Test
    void testDisableTwoFactor_ValidTotpCode_Success() throws Exception {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);
        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        boolean result = twoFactorService.disableTwoFactor(user, TwoFactorMethod.TOTP, validCode);

        assertTrue(result);
        assertFalse(user.isTwoFactorEnabled());
        assertFalse(user.isTotpEnabled());
        assertNull(user.getTotpSecret());
        assertNull(user.getLastTotpStep());
        verify(userRepository, times(1)).persist(user);
        verify(backupCodeRepository, times(1)).deleteByUser(user);
        verify(challengeRepository, times(1)).deleteByUser(user);
    }

    @Test
    void testDisableTwoFactor_InvalidCode_ReturnsFalseAndStaysEnabled() {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of());

        boolean result = twoFactorService.disableTwoFactor(user, TwoFactorMethod.TOTP, "000000");

        assertFalse(result);
        assertTrue(user.isTwoFactorEnabled());
        assertTrue(user.isTotpEnabled());
        assertNotNull(user.getTotpSecret());
        verify(backupCodeRepository, never()).deleteByUser(user);
        verify(challengeRepository, never()).deleteByUser(user);
    }

    @Test
    void testDisableTwoFactor_ValidBackupCode_Success() {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setEmailEnabled(true);

        String rawBackupCode = "ABCD-1234";
        String hash = BcryptUtil.bcryptHash("ABCD1234");
        TwoFactorBackupCode backupCodeEntity = new TwoFactorBackupCode(user, hash);
        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of(backupCodeEntity));

        boolean result =
                twoFactorService.disableTwoFactor(user, TwoFactorMethod.EMAIL, rawBackupCode);

        assertTrue(result);
        assertTrue(backupCodeEntity.isUsed());
        assertFalse(user.isTwoFactorEnabled());
        assertFalse(user.isEmailEnabled());
    }

    @Test
    void testVerifyCurrentTwoFactorCode_TooManyFailedAttempts_ThrowsAccountLocked() {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        when(twoFactorAttemptRepository.countFailedAttempts(eq(user), any(Instant.class)))
                .thenReturn(5L);

        // Guards both the /2fa/disable and email-change flows -- a hijacked session must not be
        // able to brute-force the current TOTP code just because the login-challenge path is
        // rate-limited.
        assertThrows(
                AccountLockedException.class,
                () -> twoFactorService.disableTwoFactor(user, TwoFactorMethod.TOTP, "000000"));
    }

    @Test
    void testVerifyCurrentTwoFactorCode_RecordsAttemptOutcome() throws Exception {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);
        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        boolean result = twoFactorService.verifyCurrentTwoFactorCode(user, validCode);

        assertTrue(result);
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, true);
    }

    @Test
    void testDisableTwoFactor_NotEnabled_IsIdempotentNoOp() {
        User user = new User();
        user.setTwoFactorEnabled(false);

        boolean result = twoFactorService.disableTwoFactor(user, TwoFactorMethod.TOTP, "anything");

        assertTrue(result);
        assertFalse(user.isTwoFactorEnabled());
    }

    @Test
    void testDisableTwoFactor_OneOfTwoActiveFactors_OtherStaysEnabled() throws Exception {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setEmailEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);
        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        // Prove possession via TOTP (still active) while disabling EMAIL specifically -- proof
        // doesn't have to come from the factor being removed.
        boolean result = twoFactorService.disableTwoFactor(user, TwoFactorMethod.EMAIL, validCode);

        assertTrue(result);
        assertFalse(user.isEmailEnabled());
        assertTrue(user.isTotpEnabled());
        assertTrue(user.isTwoFactorEnabled());
        verify(backupCodeRepository, never()).deleteByUser(user);
        verify(challengeRepository, never()).deleteByUser(user);
    }

    @Test
    void testRegenerateBackupCodes_ValidTotpCode_Success() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);
        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        TwoFactorBackupCodesDTO dto = twoFactorService.regenerateBackupCodes(user, validCode);

        assertNotNull(dto);
        assertNotNull(dto.backupCodes());
        assertEquals(8, dto.backupCodes().size());
        verify(backupCodeRepository, times(1)).deleteByUser(user);
        verify(backupCodeRepository, times(8)).persist(any(TwoFactorBackupCode.class));
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, true);
    }

    @Test
    void testRegenerateBackupCodes_InvalidCode_ThrowsAndDoesNotRegenerate() {
        User user = new User();
        user.setUsername("testuser");
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of());

        // Without proof of current 2FA possession, regenerating would let a hijacked session
        // invalidate the real backup codes and mint a fresh set it controls.
        assertThrows(
                de.felixhertweck.seatreservation.security.exceptions.InvalidTwoFactorCodeException
                        .class,
                () -> twoFactorService.regenerateBackupCodes(user, "000000"));

        verify(backupCodeRepository, never()).deleteByUser(user);
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, false);
    }

    @Test
    void testRegenerateBackupCodes_TooManyFailedAttempts_ThrowsAccountLocked() {
        User user = new User();
        user.setUsername("testuser");
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        when(twoFactorAttemptRepository.countFailedAttempts(eq(user), any(Instant.class)))
                .thenReturn(5L);

        assertThrows(
                AccountLockedException.class,
                () -> twoFactorService.regenerateBackupCodes(user, "000000"));

        verify(backupCodeRepository, never()).deleteByUser(user);
    }

    @Test
    void testCreateChallenge_EmailEnabled_SendsEmailCode() {
        User user = new User();
        user.setUsername("emailuser");
        user.setEmailEnabled(true);

        TwoFactorChallenge challenge = twoFactorService.createChallenge(user);

        assertNotNull(challenge);
        assertNotNull(challenge.getChallengeToken());
        assertNotNull(challenge.getEmailCode());
        assertEquals(6, challenge.getEmailCode().length());
        verify(challengeRepository, times(1)).deleteByUser(user);
        verify(challengeRepository, times(1)).persist(challenge);
        verify(emailService, times(1)).sendTwoFactorCode(eq(user), eq(challenge.getEmailCode()));
    }

    @Test
    void testCreateChallenge_TotpOnly_NoEmailCode() {
        User user = new User();
        user.setUsername("totpuser");
        user.setTotpEnabled(true);

        TwoFactorChallenge challenge = twoFactorService.createChallenge(user);

        assertNotNull(challenge);
        assertNotNull(challenge.getChallengeToken());
        assertNull(challenge.getEmailCode());
        verify(challengeRepository, times(1)).deleteByUser(user);
        verify(challengeRepository, times(1)).persist(challenge);
        verify(emailService, never()).sendTwoFactorCode(any(), any());
    }

    @Test
    void testCreateChallenge_BothEnabled_SendsEmailCodeAndAcceptsEither() {
        User user = new User();
        user.setUsername("bothuser");
        user.setTotpEnabled(true);
        user.setEmailEnabled(true);

        TwoFactorChallenge challenge = twoFactorService.createChallenge(user);

        assertNotNull(challenge.getEmailCode());
        verify(emailService, times(1)).sendTwoFactorCode(eq(user), eq(challenge.getEmailCode()));
    }

    @Test
    void testResendEmailCode() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.setUsername("emailuser");

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", "111111", Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));

        twoFactorService.resendEmailCode("token123");

        verify(challengeRepository, times(1)).persist(challenge);
        verify(emailService, times(1)).sendTwoFactorCode(eq(user), any(String.class));
    }

    @Test
    void testResendEmailCode_OnCooldown_DoesNotResend() {
        User user = new User();
        user.id = UUID.randomUUID();
        user.setUsername("emailuser");

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", "111111", Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));
        when(emailCooldownService.checkAndRecord(
                        EmailCooldownService.Purpose.TWO_FACTOR_RESEND, user.id.toString()))
                .thenReturn(Optional.of(Instant.now().plusSeconds(30)));

        twoFactorService.resendEmailCode("token123");

        verify(challengeRepository, never()).persist(any(TwoFactorChallenge.class));
        verify(emailService, never()).sendTwoFactorCode(any(), any());
    }

    @Test
    void testVerifyChallengeAndGetUser_EmailSuccess() {
        User user = new User();
        user.setUsername("emailuser");
        user.setEmailEnabled(true);

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", "999888", Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));

        Optional<User> opt = twoFactorService.verifyChallengeAndGetUser("token123", "999888");

        assertTrue(opt.isPresent());
        assertEquals("emailuser", opt.get().getUsername());
        assertTrue(challenge.isUsed());
        verify(challengeRepository, times(1)).persist(challenge);
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, true);
    }

    @Test
    void testVerifyChallengeAndGetUser_BackupCodeFallback() {
        User user = new User();
        user.setUsername("backupuser");
        user.setTotpEnabled(true);

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", null, Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));

        String rawBackupCode = "ABCD-1234";
        String normalized = "ABCD1234";
        String hash = BcryptUtil.bcryptHash(normalized);
        TwoFactorBackupCode backupCodeEntity = new TwoFactorBackupCode(user, hash);

        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of(backupCodeEntity));

        Optional<User> opt = twoFactorService.verifyChallengeAndGetUser("token123", rawBackupCode);

        assertTrue(opt.isPresent());
        assertTrue(backupCodeEntity.isUsed());
        assertTrue(challenge.isUsed());
    }

    @Test
    void testVerifyChallengeAndGetUser_ExpiredChallenge_ReturnsEmpty() {
        User user = new User();
        user.setUsername("expireduser");

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", "123456", Instant.now().minusSeconds(10));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));

        Optional<User> opt = twoFactorService.verifyChallengeAndGetUser("token123", "123456");

        assertTrue(opt.isEmpty());
        verify(twoFactorAttemptRepository, never()).recordAttempt(any(), any(Boolean.class));
    }

    @Test
    void testVerifyChallengeAndGetUser_TotpSuccess() throws Exception {
        User user = new User();
        user.setUsername("totpuser");
        user.setTotpEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);

        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", null, Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));

        Optional<User> opt = twoFactorService.verifyChallengeAndGetUser("token123", validCode);

        assertTrue(opt.isPresent());
        assertEquals("totpuser", opt.get().getUsername());
        assertTrue(challenge.isUsed());
        assertEquals(user.getLastTotpStep(), opt.get().getLastTotpStep());
        assertNotNull(opt.get().getLastTotpStep());
        verify(challengeRepository, times(1)).persist(challenge);
    }

    @Test
    void testVerifyChallengeAndGetUser_BothEnabled_EitherCodeCompletesChallenge() {
        User user = new User();
        user.setUsername("bothuser");
        user.setTotpEnabled(true);
        user.setEmailEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", "654321", Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));

        // Neither a TOTP secret match nor a backup code -- only the email code is right, and it
        // must still be accepted even though TOTP is also an active factor.
        Optional<User> opt = twoFactorService.verifyChallengeAndGetUser("token123", "654321");

        assertTrue(opt.isPresent());
        assertTrue(challenge.isUsed());
    }

    @Test
    void testVerifyChallengeAndGetUser_TotpCodeCannotBeReplayed() throws Exception {
        User user = new User();
        user.setUsername("totpuser");
        user.setTotpEnabled(true);
        String secret = TwoFactorService.encodeBase32(TOTP_SECRET_BYTES);
        user.setTotpSecret(secret);
        String validCode = validTotpCodeFor(TOTP_SECRET_BYTES);

        TwoFactorChallenge firstChallenge =
                new TwoFactorChallenge(user, "token1", null, Instant.now().plusSeconds(300));
        TwoFactorChallenge secondChallenge =
                new TwoFactorChallenge(user, "token2", null, Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token1"))
                .thenReturn(Optional.of(firstChallenge));
        when(challengeRepository.findByChallengeToken("token2"))
                .thenReturn(Optional.of(secondChallenge));
        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of());

        Optional<User> firstAttempt =
                twoFactorService.verifyChallengeAndGetUser("token1", validCode);
        assertTrue(firstAttempt.isPresent());

        // Same code replayed against a brand new challenge must be rejected.
        Optional<User> replay = twoFactorService.verifyChallengeAndGetUser("token2", validCode);
        assertTrue(replay.isEmpty());
    }

    @Test
    void testVerifyChallengeAndGetUser_WrongCode_ReturnsEmpty() {
        User user = new User();
        user.setUsername("totpuser");
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", null, Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));
        when(backupCodeRepository.findUnusedByUser(user)).thenReturn(List.of());

        Optional<User> opt = twoFactorService.verifyChallengeAndGetUser("token123", "000000");

        assertTrue(opt.isEmpty());
        assertFalse(challenge.isUsed());
        verify(challengeRepository, never()).persist(challenge);
        verify(twoFactorAttemptRepository, times(1)).recordAttempt(user, false);
    }

    @Test
    void testVerifyChallengeAndGetUser_TooManyFailedAttempts_ThrowsAccountLocked() {
        User user = new User();
        user.setUsername("lockeduser");
        user.setTotpEnabled(true);
        user.setTotpSecret("JBSWY3DPEHPK3PXP");

        TwoFactorChallenge challenge =
                new TwoFactorChallenge(user, "token123", null, Instant.now().plusSeconds(300));

        when(challengeRepository.findByChallengeToken("token123"))
                .thenReturn(Optional.of(challenge));
        when(twoFactorAttemptRepository.countFailedAttempts(eq(user), any(Instant.class)))
                .thenReturn(5L);

        assertThrows(
                AccountLockedException.class,
                () -> twoFactorService.verifyChallengeAndGetUser("token123", "000000"));

        // Locked out before ever attempting to verify the code or consume a backup code.
        verify(backupCodeRepository, never()).findUnusedByUser(user);
        verify(twoFactorAttemptRepository, never()).recordAttempt(any(), any(Boolean.class));
    }

    @Test
    void testChallengeIfRequired_NotEnabled_ReturnsEmpty() {
        User user = new User();
        user.setTwoFactorEnabled(false);

        Optional<TwoFactorRequiredDTO> result = twoFactorService.challengeIfRequired(user);

        assertTrue(result.isEmpty());
        verify(challengeRepository, never()).persist(any(TwoFactorChallenge.class));
    }

    @Test
    void testChallengeIfRequired_Enabled_ReturnsChallenge() {
        User user = new User();
        user.setUsername("totpuser");
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);

        Optional<TwoFactorRequiredDTO> result = twoFactorService.challengeIfRequired(user);

        assertTrue(result.isPresent());
        assertTrue(result.get().twoFactorRequired());
        assertNotNull(result.get().challengeToken());
        assertTrue(result.get().totpAvailable());
        assertFalse(result.get().emailAvailable());
        verify(challengeRepository, times(1)).persist(any(TwoFactorChallenge.class));
    }

    @Test
    void testChallengeIfRequired_BothEnabled_BothAvailable() {
        User user = new User();
        user.setUsername("bothuser");
        user.setEmail("bothuser@example.com");
        user.setTwoFactorEnabled(true);
        user.setTotpEnabled(true);
        user.setEmailEnabled(true);

        Optional<TwoFactorRequiredDTO> result = twoFactorService.challengeIfRequired(user);

        assertTrue(result.isPresent());
        assertTrue(result.get().totpAvailable());
        assertTrue(result.get().emailAvailable());
    }

    @Test
    void testChallengeIfRequiredAfterPasskeyLogin_NotEnabled_ReturnsEmpty() {
        User user = new User();
        user.setTwoFactorEnabled(false);
        user.setTwoFactorPasskeyEnabled(true);

        Optional<TwoFactorRequiredDTO> result =
                twoFactorService.challengeIfRequiredAfterPasskeyLogin(user);

        assertTrue(result.isEmpty());
        verify(challengeRepository, never()).persist(any(TwoFactorChallenge.class));
    }

    @Test
    void testChallengeIfRequiredAfterPasskeyLogin_PasskeyGateOff_ReturnsEmpty() {
        User user = new User();
        user.setTwoFactorEnabled(true);
        user.setTwoFactorPasskeyEnabled(false);

        Optional<TwoFactorRequiredDTO> result =
                twoFactorService.challengeIfRequiredAfterPasskeyLogin(user);

        assertTrue(result.isEmpty());
        verify(challengeRepository, never()).persist(any(TwoFactorChallenge.class));
    }

    @Test
    void testChallengeIfRequiredAfterPasskeyLogin_Gated_ReturnsChallenge() {
        User user = new User();
        user.setUsername("passkeyuser");
        user.setTwoFactorEnabled(true);
        user.setTwoFactorPasskeyEnabled(true);
        user.setEmailEnabled(true);
        user.setEmail("passkeyuser@example.com");

        Optional<TwoFactorRequiredDTO> result =
                twoFactorService.challengeIfRequiredAfterPasskeyLogin(user);

        assertTrue(result.isPresent());
        assertTrue(result.get().twoFactorRequired());
        assertNotNull(result.get().challengeToken());
        assertTrue(result.get().emailAvailable());
        assertFalse(result.get().totpAvailable());
        verify(challengeRepository, times(1)).persist(any(TwoFactorChallenge.class));
        verify(emailService, times(1)).sendTwoFactorCode(eq(user), any(String.class));
    }

    @Test
    void testBase32EncodingDecoding() {
        byte[] original = "Hello 2FA World!".getBytes();
        String encoded = TwoFactorService.encodeBase32(original);
        assertNotNull(encoded);
        assertFalse(encoded.contains("="), "encoded secret should not carry padding");

        byte[] decoded = TwoFactorService.decodeBase32(encoded);
        assertEquals(new String(original), new String(decoded));
    }
}
