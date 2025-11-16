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
package de.felixhertweck.seatreservation.userManagment.resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/**
 * REST resource for user management operations. Provides endpoints for creating, updating,
 * deleting, and retrieving users. Allows administrators to manage users and users to manage their
 * own profiles.
 */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject UserService userService;
    @Inject SecurityContext securityContext;
    @Inject UserSecurityContext userSecurityContext;

    /**
     * Imports a batch of users from the provided DTOs.
     *
     * @param userCreationDTOs the set of user creation DTOs to import
     * @return a set of created UserDTOs
     */
    @POST
    @Path("/admin/import")
    @RolesAllowed(Roles.ADMIN)
    @APIResponse(responseCode = "200", description = "Users imported successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid user data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN role can access this resource")
    @APIResponse(
            responseCode = "409",
            description =
                    "Conflict: One or more users in the batch have a conflicting username or email")
    public Set<UserDTO> importUsers(@Valid Set<AdminUserCreationDto> userCreationDTOs) {
        LOG.debugf(
                "Received POST request to /api/users/admin/import for %d users.",
                userCreationDTOs.size());
        Set<UserDTO> importedUsers = userService.importUsers(userCreationDTOs);
        return importedUsers;
    }

    /**
     * Creates a new user with the provided creation details.
     *
     * @param userCreationDTO the user creation DTO containing user details
     * @return the created UserDTO
     */
    @POST
    @Path("/admin")
    @RolesAllowed(Roles.ADMIN)
    @APIResponse(responseCode = "201", description = "User created successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid user data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN role can access this resource")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: User with this username already exists")
    public UserDTO createUser(@Valid AdminUserCreationDto userCreationDTO) {
        LOG.debugf(
                "Received POST request to /api/users/admin for user: %s",
                userCreationDTO.getUsername());
        UserDTO createdUser =
                userService.createUser(
                        new UserCreationDTO(userCreationDTO),
                        userCreationDTO.getRoles(),
                        userCreationDTO.getSendEmailVerification());
        LOG.debugf("User %s created successfully by admin.", createdUser.username());
        return createdUser;
    }

    /**
     * Updates an existing user with the provided update details.
     *
     * @param id the ID of the user to update
     * @param user the user update DTO containing updated user details
     * @return the updated UserDTO
     */
    @PUT
    @Path("/admin/{id}")
    @RolesAllowed(Roles.ADMIN)
    @APIResponse(responseCode = "200", description = "User updated successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid user data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN role can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: User with specified ID not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: User with this username already exists")
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error: Error sending email confirmation")
    public UserDTO updateUser(@PathParam("id") Long id, @Valid AdminUserUpdateDTO user) {
        LOG.debugf("Received PUT request to /api/users/admin/%d for user update.", id);
        UserDTO updatedUser = userService.updateUser(id, user);
        LOG.debugf("User with ID %d updated successfully by admin.", id);
        return updatedUser;
    }

    /**
     * Deletes a user by ID.
     *
     * @param id the ID of the user to delete
     */
    @DELETE
    @Path("/admin/{id}")
    @RolesAllowed(Roles.ADMIN)
    @APIResponse(responseCode = "204", description = "User deleted successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN role can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: User with specified ID not found")
    public void deleteUser(@QueryParam("ids") List<Long> ids) {
        LOG.debugf(
                "Received DELETE request to /api/users/admin/%d for user deletion.",
                ids != null ? ids : Collections.emptyList());
        userService.deleteUser(ids);
        LOG.debugf(
                "User with ID %d deleted successfully by admin.",
                ids != null ? ids : Collections.emptyList());
    }

    /**
     * Gets all users with limited information (for managers and admins).
     *
     * @return a list of LimitedUserInfoDTOs
     */
    @GET
    @Path("/manager")
    @RolesAllowed({Roles.ADMIN, Roles.MANAGER})
    @APIResponse(
            responseCode = "200",
            description = "List of users (limited info) retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN or MANAGER roles can access this resource")
    public List<LimitedUserInfoDTO> getAllUsers() {
        LOG.debugf("Received GET request to /api/users/manager to get all users (limited info).");
        List<LimitedUserInfoDTO> users = userService.getAllUsers();
        LOG.debugf("Returning %d limited user info DTOs.", users.size());
        return users;
    }

    /**
     * Gets the list of available roles.
     *
     * @return a list of available role names
     */
    @GET
    @Authenticated
    @Path("/roles")
    @APIResponse(
            responseCode = "200",
            description = "List of available roles retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public List<String> availableRoles() {
        LOG.debugf("Received GET request to /api/users/roles to get available roles.");
        List<String> roles = userService.getAvailableRoles();
        LOG.debugf("Returning %d available roles.", roles.size());
        return roles;
    }

    /**
     * Gets all users with full details (admin view).
     *
     * @return a list of UserDTOs
     */
    @GET
    @Path("/admin")
    @RolesAllowed(Roles.ADMIN)
    @APIResponse(
            responseCode = "200",
            description = "List of users (admin view) retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN role can access this resource")
    public List<UserDTO> getAllUsersAsAdmin() {
        LOG.debugf("Received GET request to /api/users/admin to get all users (admin view).");
        List<UserDTO> users = userService.getUsersAsAdmin();
        LOG.debugf("Returning %d user DTOs for admin view.", users.size());
        return users;
    }

    /**
     * Updates the current authenticated user's profile.
     *
     * @param userProfileUpdateDTO the user profile update DTO
     * @return the updated UserDTO
     */
    @PUT
    @Path("/me")
    @Authenticated
    @APIResponse(responseCode = "200", description = "User profile updated successfully")
    @APIResponse(responseCode = "400", description = "Bad Request: Invalid user data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: User not found")
    @APIResponse(
            responseCode = "409",
            description = "Conflict: User with this username already exists")
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error: Error sending email confirmation")
    public UserDTO updateCurrentUserProfile(@Valid UserProfileUpdateDTO userProfileUpdateDTO) {
        String username = securityContext.getUserPrincipal().getName();
        LOG.debugf(
                "Received PUT request to /api/users/me to update profile for user: %s", username);
        UserDTO updatedUser = userService.updateUserProfile(username, userProfileUpdateDTO);
        LOG.debugf("User profile for %s updated successfully.", username);
        return updatedUser;
    }

    /**
     * Gets the current authenticated user's profile information.
     *
     * @return the current UserDTO
     */
    @GET
    @Path("/me")
    @Authenticated
    @APIResponse(responseCode = "200", description = "Current user profile retrieved successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only authenticated users can access this resource")
    public UserDTO getCurrentUser() {
        String username = securityContext.getUserPrincipal().getName();
        LOG.debugf("Received GET request to /api/users/me for current user: %s", username);
        UserDTO currentUser = new UserDTO(userSecurityContext.getCurrentUser());
        LOG.debugf("Returning current user DTO for %s.", username);
        return currentUser;
    }
}
