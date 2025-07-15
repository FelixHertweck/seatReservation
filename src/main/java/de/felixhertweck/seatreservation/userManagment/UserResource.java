package de.felixhertweck.seatreservation.userManagment;

import java.util.Arrays;
import java.util.List;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;

import de.felixhertweck.seatreservation.entity.User;
import de.felixhertweck.seatreservation.repository.UserRepository;
import de.felixhertweck.seatreservation.security.Roles;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserRepository userRepository;

    @POST
    @Transactional
    @RolesAllowed(Roles.ADMIN)
    public Response createUser(User user) {
        userRepository.persist(user);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed(Roles.ADMIN)
    public Response updateUser(@PathParam("id") Long id, User user) {
        User existingUser = userRepository.findById(id);
        if (existingUser == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existingUser.setFirstname(user.getFirstname());
        existingUser.setLastname(user.getLastname());
        existingUser.setPasswordHash(user.getPasswordHash()); // In a real app, hash this!
        existingUser.setRoles(user.getRoles());
        userRepository.persist(existingUser);
        return Response.ok(existingUser).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed(Roles.ADMIN)
    public Response deleteUser(@PathParam("id") Long id) {
        boolean deleted = userRepository.deleteById(id);
        if (deleted) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @RolesAllowed(Roles.ADMIN)
    public List<User> getAllUsers() {
        return userRepository.listAll();
    }

    @GET
    @RolesAllowed(Roles.ADMIN)
    public List<String> availableRoles() {
        return Arrays.asList(Roles.ALL_ROLES);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    public Response getUserById(@PathParam("id") Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(user).build();
    }
}
