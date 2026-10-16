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
package de.felixhertweck.seatreservation.security.service;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.LoginAttemptRepository;
import de.felixhertweck.seatreservation.model.repository.UserRepository;
import de.felixhertweck.seatreservation.security.exceptions.AccountLockedException;
import de.felixhertweck.seatreservation.security.exceptions.AuthenticationFailedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Login against the real database with a username spelled differently than stored. */
@QuarkusTest
public class AuthServiceCaseInsensitiveLoginTest {

    private static final String USERNAME = "CaseLoginUser";
    private static final String PASSWORD = "testpassword";

    @Inject AuthService authService;

    @Inject UserRepository userRepository;

    @Inject LoginAttemptRepository loginAttemptRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        cleanUp();
        String salt = "randomSalt";
        userRepository.persist(
                new User(
                        USERNAME,
                        "caselogin@example.com",
                        true,
                        false,
                        BcryptUtil.bcryptHash(PASSWORD + salt),
                        salt,
                        "Case",
                        "Login",
                        Set.of(Roles.USER),
                        null));
    }

    @AfterEach
    @Transactional
    void cleanUp() {
        loginAttemptRepository.delete("lower(username)", USERNAME.toLowerCase());
        userRepository.delete("lower(username)", USERNAME.toLowerCase());
    }

    @Test
    void authenticate_SucceedsWithDifferentCase() throws AuthenticationFailedException {
        User user = authService.authenticate(USERNAME.toUpperCase(), PASSWORD);

        assertEquals(USERNAME, user.getUsername());
    }

    @Test
    void authenticate_LockoutCountsAttemptsAcrossSpellings() {
        for (String spelling :
                new String[] {USERNAME, USERNAME.toLowerCase(), USERNAME.toUpperCase()}) {
            assertThrows(
                    AuthenticationFailedException.class,
                    () -> authService.authenticate(spelling, "wrong"));
        }

        assertThrows(
                AccountLockedException.class,
                () -> authService.authenticate(USERNAME.toLowerCase(), PASSWORD));
    }
}
