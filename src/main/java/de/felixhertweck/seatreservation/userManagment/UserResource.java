package de.felixhertweck.seatreservation.userManagment;

import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.common.dto.LimitedUserInfoDTO;
import de.felixhertweck.seatreservation.common.dto.UserDTO;
import de.felixhertweck.seatreservation.security.Roles;
import de.felixhertweck.seatreservation.userManagment.dto.UserCreationDTO;
import de.felixhertweck.seatreservation.userManagment.dto.UserProfileUpdateDTO;
import de.felixhertweck.seatreservation.userManagment.exceptions.DuplicateUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.InvalidUserException;
import de.felixhertweck.seatreservation.userManagment.exceptions.UserNotFoundException;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserService userService;
    @Inject SecurityContext securityContext;

    @POST
    @RolesAllowed(Roles.ADMIN)
    public Response createUser(UserCreationDTO userCreationDTO) {
        try {
            UserDTO createdUser = userService.createUser(userCreationDTO);
            return Response.status(Response.Status.CREATED).entity(createdUser).build();
        } catch (InvalidUserException | DuplicateUserException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    public Response updateUser(@PathParam("id") Long id, UserProfileUpdateDTO user) {
        try {
            UserDTO updatedUser = userService.updateUser(id, user);
            return Response.ok(updatedUser).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (InvalidUserException | DuplicateUserException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    public Response deleteUser(@PathParam("id") Long id) {
        try {
            userService.deleteUser(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @RolesAllowed({Roles.ADMIN, Roles.MANAGER})
    public List<LimitedUserInfoDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GET
    @RolesAllowed(Roles.ADMIN)
    @Path("/roles")
    public List<String> availableRoles() {
        return userService.getAvailableRoles();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    public Response getUserById(@PathParam("id") Long id) {
        try {
            UserDTO user = userService.getUserById(id);
            return Response.ok(user).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/me")
    @RolesAllowed({Roles.USER})
    public Response updateCurrentUserProfile(UserProfileUpdateDTO userProfileUpdateDTO) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            UserDTO updatedUser = userService.updateUserProfile(username, userProfileUpdateDTO);
            return Response.ok(updatedUser).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (InvalidUserException | DuplicateUserException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
