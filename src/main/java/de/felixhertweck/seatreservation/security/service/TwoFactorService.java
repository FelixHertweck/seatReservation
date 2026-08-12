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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.zxing.WriterException;
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
import de.felixhertweck.seatreservation.security.exceptions.InvalidTwoFactorCodeException;
import de.felixhertweck.seatreservation.security.exceptions.TwoFactorAlreadyEnabledException;
import de.felixhertweck.seatreservation.utils.QRCodeImage;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.apache.commons.codec.binary.Base32;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TwoFactorService {

    private static final Logger LOG = Logger.getLogger(TwoFactorService.class);
    private static final Base32 BASE32_CODEC = new Base32();

    @Inject UserRepository userRepository;
    @Inject TwoFactorBackupCodeRepository backupCodeRepository;
    @Inject TwoFactorChallengeRepository challengeRepository;
    @Inject TwoFactorAttemptRepository twoFactorAttemptRepository;
    @Inject EmailService emailService;
    @Inject EmailCooldownService emailCooldownService;
    @Inject @Any Instance<SecondFactor> secondFactors;

    public Iterable<SecondFactor> getSecondFactors() {
        return secondFactors;
    }

    public Optional<SecondFactor> getSecondFactor(TwoFactorMethod method) {
        for (SecondFactor sf : getSecondFactors()) {
            if (sf.method() == method) {
                return Optional.of(sf);
            }
        }
        return Optional.empty();
    }

    @ConfigProperty(name = "two-factor.max-failed-attempts", defaultValue = "5")
    int maxFailedAttempts;

    @ConfigProperty(name = "two-factor.lockout-duration-seconds", defaultValue = "300")
    int lockoutDurationSeconds;

    public TwoFactorStatusDTO getStatus(User user) {
        return buildStatus(user, null);
    }

    private TwoFactorStatusDTO buildStatus(User user, List<String> freshBackupCodes) {
        long remaining = backupCodeRepository.countUnusedByUser(user);
        return new TwoFactorStatusDTO(
                user.isTwoFactorEnabled(),
                user.isTotpEnabled(),
                user.isEmailEnabled(),
                user.isTwoFactorPasskeyEnabled(),
                user.getTotpSecret() != null,
                remaining,
                freshBackupCodes);
    }

    /**
     * Initiates TOTP setup. Refuses if TOTP is already enabled -- re-enrolling would silently
     * replace the active secret, and unlike {@link #disableTwoFactor}, this call requires no proof
     * of the current factor, so allowing it here would let a hijacked session strip/replace 2FA
     * just as effectively as disabling it outright. Callers must disable TOTP (which does require
     * that proof) before setting it up again.
     *
     * @throws TwoFactorAlreadyEnabledException if TOTP is already enabled for this user
     */
    @Transactional
    public TwoFactorSetupDTO setupTotp(User user) {
        if (user.isTotpEnabled()) {
            throw new TwoFactorAlreadyEnabledException(
                    "TOTP is already enabled. Disable it before setting up a new authenticator.");
        }

        byte[] secretBytes = new byte[20];
        SecurityUtils.getSecureRandom().nextBytes(secretBytes);
        String secret = encodeBase32(secretBytes);

        String appName = "SeatReservation";
        String label = appName + ":" + user.getUsername();
        String otpauthUrl =
                String.format("otpauth://totp/%s?secret=%s&issuer=%s", label, secret, appName);

        String qrCodeDataUrl = "";
        try {
            byte[] qrBytes = QRCodeImage.generateQrCodeImage(otpauthUrl, 200, 200);
            qrCodeDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
        } catch (WriterException | IOException e) {
            LOG.error("Failed to generate QR code for TOTP", e);
        }

        // Only mint fresh backup codes if the user has none yet, so a repeated/aborted setup call
        // can't wipe out ones already saved.
        List<String> backupCodes = null;
        if (backupCodeRepository.countUnusedByUser(user) == 0) {
            backupCodes = generateBackupCodes(user);
        }

        user.setTotpSecret(secret);
        // A new secret invalidates any previously tracked step: replay protection must not carry
        // over across a re-enrollment, or a legitimately fresh code could be rejected.
        user.setLastTotpStep(null);
        saveUser(user);

        return new TwoFactorSetupDTO(secret, otpauthUrl, qrCodeDataUrl, backupCodes);
    }

    @Transactional
    public void sendSetupEmailCode(User user) {
        challengeRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        String code = String.format("%06d", SecurityUtils.nextInt(1000000));
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

        TwoFactorChallenge challenge = new TwoFactorChallenge(user, token, code, expiresAt);
        challengeRepository.persist(challenge);

        emailService.sendTwoFactorCode(user, code);
    }

    /**
     * Turns on the given factor (TOTP or EMAIL) in addition to whatever is already active -- TOTP
     * and email are independent, equally valid factors, so enabling one never disturbs the other.
     *
     * <p>For TOTP, {@code code} must be a valid code proving possession of the provisioned secret.
     * For EMAIL, no code is needed: the account's email address must already be verified (via the
     * regular account email verification flow, not a separate 2FA setup code) -- possession of that
     * address was already proven there, so requiring proof again here would just be friction. If it
     * isn't verified yet, this throws instead of silently failing, so the caller can point the user
     * at email verification specifically rather than a generic "invalid code" error.
     *
     * @return the updated status (with freshly generated backup codes if this call minted the
     *     user's first set), or empty if the TOTP code was invalid
     * @throws EmailNotVerifiedException if enabling EMAIL and the account email isn't verified
     * @throws AccountLockedException if enabling TOTP and too many failed codes were submitted
     *     recently -- same lockout as {@link #verifyCurrentTwoFactorCode}, since this is just as
     *     much a code-guessing surface (1e6 possibilities) as disabling or the login challenge.
     */
    @Transactional
    public Optional<TwoFactorStatusDTO> enableTwoFactor(
            User user, TwoFactorMethod method, String code) {
        if (method == TwoFactorMethod.TOTP) {
            if (user.getTotpSecret() == null) {
                return Optional.empty();
            }
            checkTwoFactorLockout(user);
            boolean valid = verifyTotpCode(user, code) || verifyAndConsumeBackupCode(user, code);
            twoFactorAttemptRepository.recordAttempt(user, valid);
            if (!valid) {
                return Optional.empty();
            }
            user.setTotpEnabled(true);
            user.setTwoFactorEnabled(true);
            saveUser(user);
            return Optional.of(buildStatus(user, null));
        } else if (method == TwoFactorMethod.EMAIL) {
            if (!user.isEmailVerified()) {
                throw new EmailNotVerifiedException(
                        "Your account email address must be verified before you can enable"
                                + " email-based 2FA.");
            }
            List<String> freshBackupCodes = null;
            if (backupCodeRepository.countUnusedByUser(user) == 0) {
                freshBackupCodes = generateBackupCodes(user);
            }
            user.setEmailEnabled(true);
            user.setTwoFactorEnabled(true);
            saveUser(user);
            return Optional.of(buildStatus(user, freshBackupCodes));
        }
        return Optional.empty();
    }

    @Transactional
    public void updateSettings(User user, Boolean passkeyEnabled) {
        if (passkeyEnabled != null) {
            user.setTwoFactorPasskeyEnabled(passkeyEnabled);
        }
        saveUser(user);
    }

    /**
     * Disables a single factor (TOTP or EMAIL), but only after proving current possession of 2FA (a
     * code from any currently active factor, or an unused backup code) -- otherwise a hijacked
     * session alone would be enough to strip 2FA from an account. If the given factor isn't
     * currently enabled, this is a no-op success (nothing to protect for that factor).
     *
     * <p>If the other factor is still active afterwards, 2FA as a whole stays enabled and backup
     * codes are kept; only once both factors are off is {@code twoFactorEnabled} cleared along with
     * backup codes and any pending login challenge.
     *
     * @return true if the factor was disabled (or already was), false if the code was invalid
     */
    @Transactional
    public boolean disableTwoFactor(User user, TwoFactorMethod method, String code) {
        Optional<SecondFactor> targetFactor = getSecondFactor(method);
        if (targetFactor.isPresent() && targetFactor.get().isEnabledFor(user)) {
            if (!verifyCurrentTwoFactorCode(user, code)) {
                return false;
            }
            targetFactor.get().disable(user);
        }

        boolean stillEnabled = user.isTotpEnabled() || user.isEmailEnabled();
        user.setTwoFactorEnabled(stillEnabled);
        saveUser(user);

        if (!stillEnabled) {
            backupCodeRepository.deleteByUser(user);
            challengeRepository.deleteByUser(user);
        }
        return true;
    }

    /**
     * Verifies a code against whichever 2FA factor(s) are currently active for the user (TOTP
     * and/or EMAIL), falling back to an unused backup code. Used to prove continued possession of
     * 2FA before a sensitive account action -- disabling a factor, or changing the account email
     * while 2FA is enabled. Subject to the same {@link #checkTwoFactorLockout} as {@link
     * #verifyChallengeAndGetUser} -- a 6-digit TOTP code has only 1e6 possibilities, and without
     * this a session-holding attacker (e.g. via a hijacked/stolen session) could brute-force it
     * through repeated disable/email-change calls with no login challenge involved at all.
     *
     * <p>Does not itself persist {@code user} -- a successful TOTP check may still advance {@code
     * lastTotpStep} in memory, so callers must persist the user afterward regardless of outcome (as
     * {@link #disableTwoFactor} and {@code UserService#updateUserProfile} both already do for their
     * own reasons) or that replay-protection advance is silently lost.
     */
    @Transactional
    public boolean verifyCurrentTwoFactorCode(User user, String code) {
        checkTwoFactorLockout(user);

        boolean verified = false;
        if (code != null && !code.isBlank()) {
            for (SecondFactor factor : getSecondFactors()) {
                if (factor.isEnabledFor(user) && factor.verify(user, code)) {
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                verified = verifyAndConsumeBackupCode(user, code);
            }
        }

        twoFactorAttemptRepository.recordAttempt(user, verified);
        return verified;
    }

    /**
     * Turns off EMAIL 2FA without requiring a code, because the caller (not the user) determined it
     * needs to happen: the account's email address just changed, so the newly-set address hasn't
     * been verified and can no longer be trusted for 2FA. No-op if EMAIL wasn't enabled.
     */
    @Transactional
    public void disableEmailFactorForEmailChange(User user) {
        if (!user.isEmailEnabled()) {
            return;
        }
        user.setEmailEnabled(false);
        boolean stillEnabled = user.isTotpEnabled();
        user.setTwoFactorEnabled(stillEnabled);
        saveUser(user);
        if (!stillEnabled) {
            backupCodeRepository.deleteByUser(user);
            challengeRepository.deleteByUser(user);
        }
    }

    private void saveUser(User user) {
        if (user.id == null) {
            userRepository.persist(user);
        } else {
            userRepository.getEntityManager().merge(user);
        }
    }

    /**
     * Regenerates backup codes, but only after proving current possession of 2FA (a code from any
     * currently active factor, or an unused backup code) -- otherwise a hijacked session alone
     * would be enough to invalidate the real backup codes and mint a fresh set it controls,
     * bypassing 2FA just as effectively as {@link #disableTwoFactor} without the code check would.
     *
     * @throws de.felixhertweck.seatreservation.security.exceptions.InvalidTwoFactorCodeException if
     *     the given code does not prove current possession of 2FA
     */
    @Transactional
    public TwoFactorBackupCodesDTO regenerateBackupCodes(User user, String code) {
        if (!verifyCurrentTwoFactorCode(user, code)) {
            throw new InvalidTwoFactorCodeException(
                    "A valid 2FA code is required to regenerate backup codes.");
        }
        // Persists the advanced lastTotpStep from the check above (see verifyCurrentTwoFactorCode's
        // javadoc), regardless of the fact that generateBackupCodes below persists other rows.
        saveUser(user);
        List<String> newCodes = generateBackupCodes(user);
        return new TwoFactorBackupCodesDTO(newCodes);
    }

    /**
     * Gates a password login behind 2FA if the user has it enabled.
     *
     * @return the challenge the client must complete, or empty if no 2FA is required
     */
    @Transactional
    public Optional<TwoFactorRequiredDTO> challengeIfRequired(User user) {
        if (!user.isTwoFactorEnabled()) {
            return Optional.empty();
        }
        return Optional.of(buildChallengeRequiredDTO(user));
    }

    /**
     * Gates a passkey login behind 2FA, but only if the user opted into requiring a second factor
     * on top of their passkey.
     *
     * @return the challenge the client must complete, or empty if no 2FA is required
     */
    @Transactional
    public Optional<TwoFactorRequiredDTO> challengeIfRequiredAfterPasskeyLogin(User user) {
        if (!user.isTwoFactorEnabled() || !user.isTwoFactorPasskeyEnabled()) {
            return Optional.empty();
        }
        return Optional.of(buildChallengeRequiredDTO(user));
    }

    private TwoFactorRequiredDTO buildChallengeRequiredDTO(User user) {
        TwoFactorChallenge challenge = createChallenge(user);
        return new TwoFactorRequiredDTO(
                true, challenge.getChallengeToken(), user.isTotpEnabled(), user.isEmailEnabled());
    }

    @Transactional
    public TwoFactorChallenge createChallenge(User user) {
        challengeRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

        // Proactively send an email code whenever email is an active factor, even if TOTP is
        // also active -- the client may let the user pick either method for this challenge.
        String emailCode = null;
        if (user.isEmailEnabled()) {
            emailCode = String.format("%06d", SecurityUtils.nextInt(1000000));
        }

        TwoFactorChallenge challenge = new TwoFactorChallenge(user, token, emailCode, expiresAt);
        challengeRepository.persist(challenge);

        if (emailCode != null) {
            emailService.sendTwoFactorCode(user, emailCode);
        }

        return challenge;
    }

    @Transactional
    public void resendEmailCode(String challengeToken) {
        Optional<TwoFactorChallenge> opt = challengeRepository.findByChallengeToken(challengeToken);
        if (opt.isPresent()) {
            TwoFactorChallenge challenge = opt.get();
            if (!challenge.isUsed() && challenge.getExpiresAt().isAfter(Instant.now())) {
                User user = challenge.getUser();
                // Keyed by user (not by challenge token), so requesting a fresh challenge --
                // which happens on every new login attempt -- does not reset the cooldown.
                if (emailCooldownService
                        .checkAndRecord(
                                EmailCooldownService.Purpose.TWO_FACTOR_RESEND, user.id.toString())
                        .isPresent()) {
                    return;
                }
                String newCode = String.format("%06d", SecurityUtils.nextInt(1000000));
                challenge.setEmailCode(newCode);
                challengeRepository.persist(challenge);
                emailService.sendTwoFactorCode(user, newCode);
            }
        }
    }

    @Transactional
    public Optional<User> verifyChallengeAndGetUser(String challengeToken, String code) {
        Optional<TwoFactorChallenge> opt = challengeRepository.findByChallengeToken(challengeToken);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        TwoFactorChallenge challenge = opt.get();
        if (challenge.isUsed() || challenge.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        User user = challenge.getUser();

        // Keyed by user (not by challenge), so requesting a fresh challenge -- which happens on
        // every new password login -- does not reset an attacker's failed-attempt budget.
        checkTwoFactorLockout(user);

        // TOTP and email are independent, equally valid factors: whichever ones are active, a
        // valid code for either completes the challenge.
        boolean verified = false;

        if (user.isTotpEnabled() && user.getTotpSecret() != null) {
            if (verifyTotpCode(user, code)) {
                verified = true;
            }
        }

        if (!verified
                && user.isEmailEnabled()
                && challenge.getEmailCode() != null
                && SecurityUtils.constantTimeEquals(challenge.getEmailCode(), code.trim())) {
            verified = true;
        }

        if (!verified) {
            if (verifyAndConsumeBackupCode(user, code)) {
                verified = true;
            }
        }

        twoFactorAttemptRepository.recordAttempt(user, verified);

        if (verified) {
            challenge.setUsed(true);
            challengeRepository.persist(challenge);
            // Persists the advanced lastTotpStep (if verification went through TOTP).
            saveUser(user);
            return Optional.of(user);
        }

        return Optional.empty();
    }

    private void checkTwoFactorLockout(User user) {
        Instant lockoutWindowStart = Instant.now().minusSeconds(lockoutDurationSeconds);
        long failedAttempts =
                twoFactorAttemptRepository.countFailedAttempts(user, lockoutWindowStart);

        if (failedAttempts >= maxFailedAttempts) {
            Instant retryAfter = calculateRemainingLockoutTime(user, lockoutWindowStart);
            LOG.warnf(
                    "2FA verification locked for user ID: %s due to %d failed attempts. Remaining"
                            + " lockout time: %s",
                    user.id, failedAttempts, retryAfter);
            throw new AccountLockedException(
                    "Too many failed 2FA attempts. Please try again later.", retryAfter);
        }
    }

    private Instant calculateRemainingLockoutTime(User user, Instant lockoutWindowStart) {
        Instant oldestFailedAttempt =
                twoFactorAttemptRepository.getOldestFailedAttemptTime(user, lockoutWindowStart);
        if (oldestFailedAttempt == null) {
            return Instant.now().plusSeconds(lockoutDurationSeconds);
        }
        return oldestFailedAttempt.plusSeconds(lockoutDurationSeconds);
    }

    /**
     * Verifies a 6-digit TOTP code for the given user, checking the current time-step and its
     * immediate neighbors (~90s window total) to tolerate clock drift.
     *
     * <p>To prevent replay of an intercepted code, a step is only accepted if it is strictly newer
     * than the last step accepted for this user; the accepted step is then recorded on the user
     * (persisted by the caller) so it can't be reused, even against a brand new challenge.
     */
    public boolean verifyTotpCode(User user, String code) {
        String secretBase32 = user.getTotpSecret();
        if (secretBase32 == null || code == null || code.trim().length() != 6) {
            return false;
        }
        try {
            byte[] keyBytes = decodeBase32(secretBase32);
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
            SecretKey key = new SecretKeySpec(keyBytes, totp.getAlgorithm());

            long stepSeconds = totp.getTimeStep().getSeconds();
            long currentStep = Instant.now().getEpochSecond() / stepSeconds;
            Long lastAcceptedStep = user.getLastTotpStep();
            String trimmedCode = code.trim();

            for (long step = currentStep - 1; step <= currentStep + 1; step++) {
                if (lastAcceptedStep != null && step <= lastAcceptedStep) {
                    continue;
                }
                String candidate =
                        totp.generateOneTimePasswordString(
                                key, Instant.ofEpochSecond(step * stepSeconds));
                if (SecurityUtils.constantTimeEquals(trimmedCode, candidate)) {
                    user.setLastTotpStep(step);
                    return true;
                }
            }
            return false;
        } catch (InvalidKeyException e) {
            LOG.error("Failed to verify TOTP code", e);
            return false;
        }
    }

    @Transactional
    public List<String> generateBackupCodes(User user) {
        backupCodeRepository.deleteByUser(user);
        List<String> plainCodes = new ArrayList<>();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        for (int i = 0; i < 8; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                if (j == 4) sb.append("-");
                sb.append(chars.charAt(SecurityUtils.nextInt(chars.length())));
            }
            String code = sb.toString();
            plainCodes.add(code);

            String codeHash = BcryptUtil.bcryptHash(normalizeCode(code));
            TwoFactorBackupCode backupCode = new TwoFactorBackupCode(user, codeHash);
            backupCodeRepository.persist(backupCode);
        }
        return plainCodes;
    }

    @Transactional
    public boolean verifyAndConsumeBackupCode(User user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = normalizeCode(code);
        List<TwoFactorBackupCode> unusedCodes = backupCodeRepository.findUnusedByUser(user);
        for (TwoFactorBackupCode backupCode : unusedCodes) {
            if (BcryptUtil.matches(normalized, backupCode.getCodeHash())) {
                backupCode.setUsed(true);
                backupCodeRepository.persist(backupCode);
                return true;
            }
        }
        return false;
    }

    private String normalizeCode(String code) {
        return code.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    public static String encodeBase32(byte[] bytes) {
        return BASE32_CODEC.encodeToString(bytes).replace("=", "");
    }

    public static byte[] decodeBase32(String base32) {
        String clean = base32.trim().toUpperCase().replaceAll("[^A-Z2-7]", "");
        int paddingLength = (8 - clean.length() % 8) % 8;
        String padded = clean + "=".repeat(paddingLength);
        return BASE32_CODEC.decode(padded);
    }
}
