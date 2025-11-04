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

import java.util.List;
import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.model.entity.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserCreationDto;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

/*
 * This class provides RESTful endpoints for user management operations.
 * It allows administrators to create, update, delete users, and manage user roles.
 * It also allows users to update their own profiles.
 */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject UserService userService;
    @Inject SecurityContext securityContext;
    @Inject UserSecurityContext userSecurityContext;

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

    @DELETE
    @Path("/admin/{id}")
    @RolesAllowed(Roles.ADMIN)
    @APIResponse(responseCode = "204", description = "User deleted successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(
            responseCode = "403",
            description = "Forbidden: Only ADMIN role can access this resource")
    @APIResponse(responseCode = "404", description = "Not Found: User with specified ID not found")
    public void deleteUser(@PathParam("id") Long id) {
        LOG.debugf("Received DELETE request to /api/users/admin/%d for user deletion.", id);
        userService.deleteUser(id);
        LOG.debugf("User with ID %d deleted successfully by admin.", id);
    }

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

    @GET
    @RolesAllowed({Roles.USER, Roles.MANAGER, Roles.ADMIN})
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

    @PUT
    @Path("/me")
    @RolesAllowed({Roles.USER, Roles.MANAGER, Roles.ADMIN})
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

    @GET
    @Path("/me")
    @RolesAllowed({Roles.USER, Roles.ADMIN, Roles.MANAGER})
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
