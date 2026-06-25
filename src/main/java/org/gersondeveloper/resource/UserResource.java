package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.UserRequest;
import org.gersondeveloper.domain.dto.response.UserResponse;
import org.gersondeveloper.domain.model.User;

import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public List<UserResponse> listActive() {
        return User.<User>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        User user = User.findById(id);
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(user)).build();
    }

    @POST
    @Transactional
    public Response create(UserRequest request) {
        User user = new User();
        user.username = request.username();
        user.address = request.address();
        user.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(user)).build();
    }

    @POST
    @Path("/batch")
    @Transactional
    public Response createBatch(List<UserRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        List<User> users = requests.stream().map(req -> {
            User user = new User();
            user.username = req.username();
            user.address = req.address();
            return user;
        }).toList();
        User.persist(users);
        List<UserResponse> body = users.stream().map(this::toResponse).toList();
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, UserRequest request) {
        User user = User.findById(id);
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        user.username = request.username();
        user.address = request.address();
        return Response.ok(toResponse(user)).build();
    }

    @PATCH
    @Path("/{id}/activate")
    @Transactional
    public Response activate(@PathParam("id") Long id) {
        User user = User.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        user.isActive = true;
        return Response.ok(toResponse(user)).build();
    }

    @PATCH
    @Path("/{id}/deactivate")
    @Transactional
    public Response deactivate(@PathParam("id") Long id) {
        User user = User.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        user.isActive = false;
        return Response.ok(toResponse(user)).build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.id, user.username, user.address, user.isActive, user.createDate);
    }
}
