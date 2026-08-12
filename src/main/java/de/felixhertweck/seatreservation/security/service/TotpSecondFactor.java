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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import de.felixhertweck.seatreservation.model.entity.TwoFactorMethod;
import de.felixhertweck.seatreservation.model.entity.User;

@ApplicationScoped
public class TotpSecondFactor implements SecondFactor {

    @Inject TwoFactorService twoFactorService;

    @Override
    public TwoFactorMethod method() {
        return TwoFactorMethod.TOTP;
    }

    @Override
    public boolean isEnabledFor(User user) {
        return user != null && Boolean.TRUE.equals(user.isTotpEnabled());
    }

    @Override
    public boolean verify(User user, String code) {
        if (!isEnabledFor(user)) {
            return false;
        }
        return twoFactorService.verifyTotpCode(user, code);
    }

    @Override
    public void disable(User user) {
        if (user != null) {
            user.setTotpEnabled(false);
            user.setTotpSecret(null);
            user.setLastTotpStep(null);
        }
    }
}
