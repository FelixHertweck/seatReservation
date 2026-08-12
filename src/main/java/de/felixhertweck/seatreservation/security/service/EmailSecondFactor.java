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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.TwoFactorChallenge;
import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.TwoFactorChallengeRepository;
import de.felixhertweck.seatreservation.utils.SecurityUtils;

@ApplicationScoped
public class EmailSecondFactor implements SecondFactor {

    @Inject TwoFactorChallengeRepository challengeRepository;

    @Override
    public TwoFactorMethod method() {
        return TwoFactorMethod.EMAIL;
    }

    @Override
    public boolean isEnabledFor(User user) {
        return user != null && Boolean.TRUE.equals(user.isEmailEnabled());
    }

    @Override
    public boolean verify(User user, String code) {
        if (!isEnabledFor(user)
                || user == null
                || user.id == null
                || code == null
                || code.isBlank()) {
            return false;
        }
        List<TwoFactorChallenge> challenges =
                challengeRepository.list("user = ?1 and used = false", user);
        for (TwoFactorChallenge ch : challenges) {
            if (ch.getEmailCode() != null
                    && SecurityUtils.constantTimeEquals(ch.getEmailCode(), code.trim())
                    && ch.getExpiresAt().isAfter(Instant.now())) {
                ch.setUsed(true);
                challengeRepository.persist(ch);
                return true;
            }
        }
        return false;
    }

    @Override
    public void disable(User user) {
        if (user != null) {
            user.setEmailEnabled(false);
        }
    }
}
