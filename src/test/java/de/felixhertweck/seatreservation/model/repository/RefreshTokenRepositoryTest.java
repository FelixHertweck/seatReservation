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
package de.felixhertweck.seatreservation.model.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.felixhertweck.seatreservation.model.entity.RefreshToken;
import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class RefreshTokenRepositoryTest {

    @Inject RefreshTokenRepository refreshTokenRepository;

    @Inject UserRepository userRepository;

    private User testUser;
    private User otherUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up existing refresh tokens
        refreshTokenRepository.deleteAll();

        // Use existing test users from import-test.sql
        testUser = userRepository.findById(1L);
        assertNotNull(testUser, "Test user 1 should exist");

        otherUser = userRepository.findById(2L);
        assertNotNull(otherUser, "Test user 2 should exist");
    }

    @Test
    @Transactional
    void testFindAllByUser_MultipleTokens() {
        // Given - Create multiple tokens for testUser
        RefreshToken token1 =
                new RefreshToken(
                        "hash1", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token1.persist();

        RefreshToken token2 =
                new RefreshToken(
                        "hash2", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token2.persist();

        // Create token for different user
        RefreshToken otherToken =
                new RefreshToken(
                        "hash3", otherUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        otherToken.persist();

        // When
        List<RefreshToken> userTokens = refreshTokenRepository.findAllByUser(testUser);

        // Then
        assertEquals(2, userTokens.size());
        assertTrue(userTokens.stream().allMatch(t -> t.getUser().id.equals(testUser.id)));
    }

    @Test
    @Transactional
    void testFindAllByUser_NoTokens() {
        // Given - No tokens for testUser

        // When
        List<RefreshToken> userTokens = refreshTokenRepository.findAllByUser(testUser);

        // Then
        assertEquals(0, userTokens.size());
    }

    @Test
    @Transactional
    void testDeleteAllByUser_Success() {
        // Given - Create multiple tokens for testUser
        RefreshToken token1 =
                new RefreshToken(
                        "hash1", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token1.persist();

        RefreshToken token2 =
                new RefreshToken(
                        "hash2", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token2.persist();

        // Create token for different user
        RefreshToken otherToken =
                new RefreshToken(
                        "hash3", otherUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        otherToken.persist();

        assertEquals(3, refreshTokenRepository.count());

        // When - Delete all tokens for testUser
        long deleted = refreshTokenRepository.deleteAllByUser(testUser);

        // Then
        assertEquals(2, deleted);
        assertEquals(1, refreshTokenRepository.count());
        // Verify other user's token still exists
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAllByUser(otherUser);
        assertEquals(1, remainingTokens.size());
    }

    @Test
    @Transactional
    void testDeleteAllByUser_NoTokens() {
        // Given - No tokens for testUser

        // When
        long deleted = refreshTokenRepository.deleteAllByUser(testUser);

        // Then
        assertEquals(0, deleted);
    }

    @Test
    @Transactional
    void testDeleteWithIdAndUser_Success() {
        // Given - Create a token for testUser
        RefreshToken token =
                new RefreshToken(
                        "hash1", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token.persist();

        Long tokenId = token.id;
        assertEquals(1, refreshTokenRepository.count());

        // When - Delete token with correct ID and user
        boolean deleted = refreshTokenRepository.deleteWithIdAndUser(tokenId, testUser);

        // Then
        assertTrue(deleted);
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteWithIdAndUser_WrongUser() {
        // Given - Create a token for testUser
        RefreshToken token =
                new RefreshToken(
                        "hash1", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token.persist();

        Long tokenId = token.id;
        assertEquals(1, refreshTokenRepository.count());

        // When - Try to delete with correct ID but wrong user
        boolean deleted = refreshTokenRepository.deleteWithIdAndUser(tokenId, otherUser);

        // Then - Token should not be deleted
        assertFalse(deleted);
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteWithIdAndUser_NonExistentId() {
        // Given - Create a token for testUser
        RefreshToken token =
                new RefreshToken(
                        "hash1", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token.persist();

        assertEquals(1, refreshTokenRepository.count());

        // When - Try to delete with non-existent ID
        boolean deleted = refreshTokenRepository.deleteWithIdAndUser(999999L, testUser);

        // Then - Token should not be deleted
        assertFalse(deleted);
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    @Transactional
    void testDeleteWithIdAndUser_OnlyDeletesSpecificToken() {
        // Given - Create multiple tokens for testUser
        RefreshToken token1 =
                new RefreshToken(
                        "hash1", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token1.persist();

        RefreshToken token2 =
                new RefreshToken(
                        "hash2", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token2.persist();

        RefreshToken token3 =
                new RefreshToken(
                        "hash3", testUser, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
        token3.persist();

        assertEquals(3, refreshTokenRepository.count());

        // When - Delete only token2
        boolean deleted = refreshTokenRepository.deleteWithIdAndUser(token2.id, testUser);

        // Then
        assertTrue(deleted);
        assertEquals(2, refreshTokenRepository.count());

        // Verify correct tokens remain
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAllByUser(testUser);
        assertEquals(2, remainingTokens.size());
        assertTrue(remainingTokens.stream().noneMatch(t -> t.id.equals(token2.id)));
    }
}
