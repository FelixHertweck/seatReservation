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
package de.felixhertweck.seatreservation.utils;

import java.util.Set;

import de.felixhertweck.seatreservation.model.entity.Roles;

/**
 * Identity and roles of the authenticated caller, derived directly from the validated JWT (id and
 * email/upn claims, groups claim) without a database round trip. Use this wherever authorization
 * logic only needs the user's ID and/or roles; fetch the full {@code User} entity (e.g. via {@code
 * UserSecurityContext#getCurrentUser()}) when other fields are required.
 */
public record AuthenticatedUser(Long id, Set<String> roles) {

    public boolean isAdmin() {
        return roles != null && roles.contains(Roles.ADMIN);
    }
}
