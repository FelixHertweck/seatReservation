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
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column private String firstname;

    @Column private String lastname;

    @Column private String passwordHash;

    @Column private String email;

    @Column private boolean emailVerified = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_tags", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "tags")
    private Set<String> tags = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventUserAllowance> eventAllowances = new HashSet<>();

    /** Constructor for JPA. */
    public User() {}

    /** Creates a new user. Constructor for application. */
    public User(
            String username,
            String email,
            boolean emailVerified,
            String passwordHash,
            String firstname,
            String lastname,
            Set<String> roles) {
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
        this.passwordHash = passwordHash;
        this.firstname = firstname;
        this.lastname = lastname;
        this.roles = new HashSet<>(roles);
    }

    public User(
            String username,
            String email,
            boolean emailVerified,
            String passwordHash,
            String firstname,
            String lastname,
            Set<String> roles,
            Set<String> tags) {
        this(username, email, emailVerified, passwordHash, firstname, lastname, roles);
        this.tags = new HashSet<>(tags);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<EventUserAllowance> getEventAllowances() {
        return eventAllowances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return emailVerified == that.emailVerified
                && Objects.equals(username, that.username)
                && Objects.equals(firstname, that.firstname)
                && Objects.equals(lastname, that.lastname)
                && Objects.equals(passwordHash, that.passwordHash)
                && Objects.equals(email, that.email)
                && Objects.equals(tags, that.tags)
                && Objects.equals(roles, that.roles)
                && Objects.equals(eventAllowances, that.eventAllowances);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(
                username,
                firstname,
                lastname,
                passwordHash,
                email,
                emailVerified,
                tags,
                roles,
                eventAllowances);
    }

    @Override
    public String toString() {
        return "User{"
                + "username='"
                + username
                + '\''
                + ", firstname='"
                + firstname
                + '\''
                + ", lastname='"
                + lastname
                + '\''
                + ", passwordHash='"
                + passwordHash
                + '\''
                + ", email='"
                + email
                + '\''
                + ", emailVerified="
                + emailVerified
                + ", tags="
                + tags
                + ", roles="
                + roles
                + ", eventAllowances="
                + eventAllowances
                + ", id="
                + id
                + '}';
    }
}
