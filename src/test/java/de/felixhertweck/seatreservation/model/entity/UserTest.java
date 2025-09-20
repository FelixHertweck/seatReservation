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
package de.felixhertweck.seatreservation.model.entity;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void testUserCreation() {
        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertFalse(user.isEmailVerified());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());
        assertNotNull(user.getTags());
        assertTrue(user.getTags().isEmpty());
    }

    @Test
    void testUsernameSetterGetter() {
        String username = "testuser";
        user.setUsername(username);
        assertEquals(username, user.getUsername());
    }

    @Test
    void testEmailSetterGetter() {
        String email = "test@example.com";
        user.setEmail(email);
        assertEquals(email, user.getEmail());
    }

    @Test
    void testEmailVerificationSetterGetter() {
        user.setEmailVerified(true);
        assertTrue(user.isEmailVerified());

        user.setEmailVerified(false);
        assertFalse(user.isEmailVerified());
    }

    @Test
    void testFirstnameSetterGetter() {
        String firstname = "John";
        user.setFirstname(firstname);
        assertEquals(firstname, user.getFirstname());
    }

    @Test
    void testLastnameSetterGetter() {
        String lastname = "Doe";
        user.setLastname(lastname);
        assertEquals(lastname, user.getLastname());
    }

    @Test
    void testPasswordHashSetterGetter() {
        String passwordHash = "$2a$10$hashedPassword";
        user.setPasswordHash(passwordHash);
        assertEquals(passwordHash, user.getPasswordHash());
    }

    @Test
    void testPasswordSaltSetterGetter() {
        String salt = "randomSalt";
        user.setPasswordSalt(salt);
        assertEquals(salt, user.getPasswordSalt());
    }

    @Test
    void testRolesSetterGetter() {
        Set<String> roles = Set.of(Roles.USER, Roles.ADMIN);
        user.setRoles(roles);
        assertEquals(roles, user.getRoles());
        assertTrue(user.getRoles().contains(Roles.USER));
        assertTrue(user.getRoles().contains(Roles.ADMIN));
    }

    @Test
    void testTagsSetterGetter() {
        Set<String> tags = Set.of("VIP", "Premium");
        user.setTags(tags);
        assertEquals(tags, user.getTags());
        assertTrue(user.getTags().contains("VIP"));
        assertTrue(user.getTags().contains("Premium"));
    }

    @Test
    void testRolesModification() {
        Set<String> roles = new HashSet<>();
        roles.add(Roles.USER);
        user.setRoles(roles);

        // Modify the roles
        user.getRoles().add(Roles.MANAGER);
        assertTrue(user.getRoles().contains(Roles.USER));
        assertTrue(user.getRoles().contains(Roles.MANAGER));
        assertEquals(2, user.getRoles().size());
    }

    @Test
    void testTagsModification() {
        Set<String> tags = new HashSet<>();
        tags.add("Basic");
        user.setTags(tags);

        // Modify the tags
        user.getTags().add("Premium");
        assertTrue(user.getTags().contains("Basic"));
        assertTrue(user.getTags().contains("Premium"));
        assertEquals(2, user.getTags().size());
    }

    @Test
    void testNullEmailHandling() {
        user.setEmail(null);
        assertNull(user.getEmail());
    }

    @Test
    void testEmptyStringHandling() {
        user.setUsername("");
        assertEquals("", user.getUsername());

        user.setEmail("");
        assertEquals("", user.getEmail());

        user.setFirstname("");
        assertEquals("", user.getFirstname());

        user.setLastname("");
        assertEquals("", user.getLastname());
    }

    @Test
    void testSpecialCharactersInFields() {
        String specialUsername = "user@domain.com"; // Username with special chars
        String specialName = "José María";

        user.setUsername(specialUsername);
        user.setFirstname(specialName);

        assertEquals(specialUsername, user.getUsername());
        assertEquals(specialName, user.getFirstname());
    }

    @Test
    void testEmailValidationPattern() {
        // Test valid email formats
        String[] validEmails = {
            "test@example.com", "user.name@domain.co.uk", "user+tag@example.org", "123@test.com"
        };

        for (String email : validEmails) {
            user.setEmail(email);
            assertEquals(email, user.getEmail());
        }
    }

    @Test
    void testRoleValidation() {
        // Test all valid roles
        Set<String> allRoles = Set.of(Roles.USER, Roles.ADMIN, Roles.MANAGER);
        user.setRoles(allRoles);

        assertEquals(3, user.getRoles().size());
        assertTrue(user.getRoles().contains(Roles.USER));
        assertTrue(user.getRoles().contains(Roles.ADMIN));
        assertTrue(user.getRoles().contains(Roles.MANAGER));
    }

    @Test
    void testEqualsAndHashCode() {
        User user1 = new User();
        user1.setUsername("testuser");
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setUsername("testuser");
        user2.setEmail("test@example.com");

        // Test equals
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());

        // Test inequality
        user2.setUsername("differentuser");
        assertNotEquals(user1, user2);
    }

    @Test
    void testToString() {
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstname("John");
        user.setLastname("Doe");

        String toString = user.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("testuser"));
    }

    @Test
    void testUserWithAllFields() {
        // Test user with all fields populated
        user.setUsername("fulluser");
        user.setEmail("full@example.com");
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setPasswordHash("$2a$10$hashedPassword");
        user.setPasswordSalt("salt123");
        user.setEmailVerified(true);
        user.setRoles(Set.of(Roles.USER, Roles.MANAGER));
        user.setTags(Set.of("VIP", "Premium"));

        assertEquals("fulluser", user.getUsername());
        assertEquals("full@example.com", user.getEmail());
        assertEquals("John", user.getFirstname());
        assertEquals("Doe", user.getLastname());
        assertEquals("$2a$10$hashedPassword", user.getPasswordHash());
        assertEquals("salt123", user.getPasswordSalt());
        assertTrue(user.isEmailVerified());
        assertEquals(2, user.getRoles().size());
        assertEquals(2, user.getTags().size());
    }

    @Test
    void testUserDefaultConstructor() {
        User newUser = new User();
        assertNotNull(newUser.getRoles());
        assertNotNull(newUser.getTags());
        assertTrue(newUser.getRoles().isEmpty());
        assertTrue(newUser.getTags().isEmpty());
        assertFalse(newUser.isEmailVerified());
    }

    @Test
    void testLongUsernameHandling() {
        String longUsername = "a".repeat(255); // Very long username
        user.setUsername(longUsername);
        assertEquals(longUsername, user.getUsername());
    }

    @Test
    void testMultipleRoleAssignments() {
        user.setRoles(Set.of(Roles.USER));
        assertEquals(1, user.getRoles().size());

        user.setRoles(Set.of(Roles.USER, Roles.ADMIN));
        assertEquals(2, user.getRoles().size());

        user.setRoles(Set.of(Roles.MANAGER));
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(Roles.MANAGER));
    }
}
