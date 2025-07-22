package de.felixhertweck.seatreservation.userManagment.resource;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.AdminUserUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.service.UserService;
import de.felixhertweck.seatreservation.utils.UserSecurityContext;

/*
 * This class provides RESTful endpoints for user management operations.
 * It allows administrators to create, update, delete users, and manage user roles.
 * It also allows users to update their own profiles.
 */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserService userService;
    @Inject SecurityContext securityContext;
    @Inject UserSecurityContext userSecurityContext;

    @POST
    @Path("/admin")
    @RolesAllowed(Roles.ADMIN)
    public UserDTO createUser(UserCreationDTO userCreationDTO) {
        return userService.createUser(userCreationDTO);
    }

    @PUT
    @Path("/admin/{id}")
    @RolesAllowed(Roles.ADMIN)
    public UserDTO updateUser(@PathParam("id") Long id, AdminUserUpdateDTO user) {
        return userService.updateUser(id, user);
    }

    @DELETE
    @Path("/admin/{id}")
    @RolesAllowed(Roles.ADMIN)
    public void deleteUser(@PathParam("id") Long id) {
        userService.deleteUser(id);
    }

    @GET
    @Path("/manager")
    @RolesAllowed({Roles.ADMIN, Roles.MANAGER})
    public List<LimitedUserInfoDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GET
    @RolesAllowed({Roles.USER, Roles.ADMIN})
    @Path("/roles")
    public List<String> availableRoles() {
        return userService.getAvailableRoles();
    }

    @GET
    @Path("/admin/{id}")
    @RolesAllowed(Roles.ADMIN)
    public UserDTO getUserById(@PathParam("id") Long id) {
        return userService.getUserById(id);
    }

    @PUT
    @Path("/me")
    @RolesAllowed({Roles.USER})
    public UserDTO updateCurrentUserProfile(UserProfileUpdateDTO userProfileUpdateDTO) {
        String username = securityContext.getUserPrincipal().getName();
        return userService.updateUserProfile(username, userProfileUpdateDTO);
    }

    @GET
    @Path("/me")
    @RolesAllowed({Roles.USER, Roles.ADMIN, Roles.MANAGER})
    public UserDTO getCurrentUser() {
        return new UserDTO(userSecurityContext.getCurrentUser());
    }
}
