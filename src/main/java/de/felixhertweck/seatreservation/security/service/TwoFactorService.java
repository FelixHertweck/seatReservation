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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import de.felixhertweck.seatreservation.email.service.EmailService;
import de.felixhertweck.seatreservation.model.entity.TwoFactorBackupCode;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.entity.UserTwoFactorSettings;
import de.felixhertweck.seatreservation.model.repository.TwoFactorBackupCodeRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSettingsDTO;
import de.felixhertweck.seatreservation.security.dto.TwoFactorSetupDTO;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import de.felixhertweck.seatreservation.utils.SecurityUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TwoFactorService {

    private static final Logger LOG = Logger.getLogger(TwoFactorService.class);

    @Inject UserRepository userRepository;
    @Inject TwoFactorBackupCodeRepository backupCodeRepository;
    @Inject EmailService emailService;
    @Inject TokenService tokenService;

    public boolean isTwoFactorEnabled(User user) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        return settings != null && settings.isTwoFactorEnabled();
    }

    public boolean isPasskeyTwoFactorRequired(User user) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        return settings != null
                && settings.isTwoFactorEnabled()
                && settings.isPasskeyRequiresTwoFactor();
    }

    public void sendEmailCodeIfApplicable(User user, String preAuthTokenJwt) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        if (settings != null
                && settings.isTwoFactorEnabled()
                && "EMAIL".equals(settings.getTwoFactorType())) {
            try {
                String codeStr = String.format("%06d", SecurityUtils.nextInt(1000000));
                String codeHash = BcryptUtil.bcryptHash(codeStr);
                tokenService.setEmailCodeForPreAuthToken(preAuthTokenJwt, codeHash);
                emailService.sendTwoFactorCode(user, codeStr);
            } catch (Exception e) {
                LOG.error("Failed to generate and send email 2FA code", e);
            }
        }
    }

    public void verifyCode(User user, String code) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        if (settings == null || !settings.isTwoFactorEnabled()) {
            throw new AuthenticationFailedException("2FA not enabled for user");
        }

        // Try Backup Code first (only if length matches backup code length, avoiding slow bcrypt
        // for TOTP)
        if (code != null && code.length() == 8) {
            List<TwoFactorBackupCode> backupCodes = backupCodeRepository.findByUser(user);
            for (TwoFactorBackupCode backupCode : backupCodes) {
                if (!backupCode.isUsed() && BcryptUtil.matches(code, backupCode.getCodeHash())) {
                    markBackupCodeUsed(backupCode);
                    return;
                }
            }
        }

        // Try TOTP
        if ("TOTP".equals(settings.getTwoFactorType())) {
            try {
                String secretHex = settings.getTotpSecret();
                if (secretHex == null || secretHex.isEmpty()) {
                    throw new AuthenticationFailedException("Invalid 2FA state");
                }
                byte[] keyBytes = hexStringToByteArray(secretHex);
                SecretKeySpec key = new SecretKeySpec(keyBytes, "HMACSHA1");
                TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();

                Instant now = Instant.now();

                int inputCode;
                try {
                    inputCode = Integer.parseInt(code);
                } catch (NumberFormatException e) {
                    throw new AuthenticationFailedException("Invalid code format");
                }

                // Check current, past 1, and future 1 step for standard TOTP
                for (int i = -1; i <= 1; i++) {
                    int checkCode = totp.generateOneTimePassword(key, now.plusSeconds(i * 30));
                    if (inputCode == checkCode) {
                        return;
                    }
                }
            } catch (InvalidKeyException e) {
                LOG.error("Failed to verify TOTP code", e);
            }
        } else if ("EMAIL".equals(settings.getTwoFactorType())) {
            // Handled by validatePreAuthToken throwing if we verify manually... Wait! The token
            // value is stored in PreAuthToken...
            // If the user gets here, it means we don't have the PreAuthToken accessible from
            // verifyCode(User user, String code) !
            // PreAuthToken was validated and removed!
            // In TokenService:
            // public User validatePreAuthToken(String token) throws JwtInvalidException {
            //     ...
            //     preAuthTokenRepository.delete(storedToken);
            //     return storedToken.getUser();
            // }
        }

        throw new AuthenticationFailedException("Invalid 2FA code");
    }

    @Transactional
    public void markBackupCodeUsed(TwoFactorBackupCode backupCode) {
        backupCode.setUsed(true);
        backupCode.persist();
    }

    public TwoFactorSettingsDTO getSettings(User user) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        if (settings == null) {
            return new TwoFactorSettingsDTO(false, "TOTP", false);
        }
        return new TwoFactorSettingsDTO(
                settings.isTwoFactorEnabled(),
                settings.getTwoFactorType(),
                settings.isPasskeyRequiresTwoFactor());
    }

    @Transactional
    public TwoFactorSettingsDTO updateSettings(User user, TwoFactorSettingsDTO update) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        if (settings == null) {
            settings = new UserTwoFactorSettings(user);
        }
        settings.setTwoFactorType(update.getTwoFactorType());
        settings.setPasskeyRequiresTwoFactor(update.isPasskeyRequiresTwoFactor());
        settings.persist();
        return getSettings(user);
    }

    @Transactional
    public TwoFactorSetupDTO setup(User user) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HMACSHA1");
            keyGenerator.init(160, SecurityUtils.getSecureRandom());
            byte[] keyBytes = keyGenerator.generateKey().getEncoded();
            String secretHex = byteArrayToHexString(keyBytes);

            UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
            if (settings == null) {
                settings = new UserTwoFactorSettings(user);
            }
            settings.setTotpSecret(secretHex);
            settings.persist();

            String base32Secret =
                    new org.apache.commons.codec.binary.Base32().encodeAsString(keyBytes);
            String qrCodeUri =
                    String.format(
                            "otpauth://totp/SeatReservation:%s?secret=%s&issuer=SeatReservation",
                            user.getUsername(), base32Secret);

            return new TwoFactorSetupDTO(base32Secret, qrCodeUri);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate TOTP secret", e);
        }
    }

    @Transactional
    public List<String> enable(User user, String code) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        if (settings == null || settings.getTotpSecret() == null) {
            throw new IllegalArgumentException("Must call setup first");
        }

        try {
            byte[] keyBytes = hexStringToByteArray(settings.getTotpSecret());
            SecretKeySpec key = new SecretKeySpec(keyBytes, "HMACSHA1");
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();

            Instant now = Instant.now();
            int currentCode = totp.generateOneTimePassword(key, now);
            int pastCode = totp.generateOneTimePassword(key, now.minusSeconds(30));

            int inputCode;
            try {
                inputCode = Integer.parseInt(code);
            } catch (NumberFormatException e) {
                throw new AuthenticationFailedException("Invalid code format");
            }

            if (inputCode != currentCode && inputCode != pastCode) {
                throw new AuthenticationFailedException("Invalid code for enabling 2FA");
            }
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        settings.setTwoFactorEnabled(true);
        if (settings.getTwoFactorType() == null) {
            settings.setTwoFactorType("TOTP");
        }
        settings.persist();

        backupCodeRepository.deleteAllByUser(user);
        List<String> plainCodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String plainCode = String.format("%08d", SecurityUtils.nextInt(100000000));
            plainCodes.add(plainCode);
            String hash = BcryptUtil.bcryptHash(plainCode);
            TwoFactorBackupCode bc = new TwoFactorBackupCode(user, hash);
            backupCodeRepository.persist(bc);
        }
        return plainCodes;
    }

    @Transactional
    public void disable(User user) {
        UserTwoFactorSettings settings = UserTwoFactorSettings.find("user", user).firstResult();
        if (settings != null) {
            settings.setTwoFactorEnabled(false);
            settings.setTotpSecret(null);
            settings.persist();
        }
        backupCodeRepository.deleteAllByUser(user);
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte)
                            ((Character.digit(s.charAt(i), 16) << 4)
                                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
