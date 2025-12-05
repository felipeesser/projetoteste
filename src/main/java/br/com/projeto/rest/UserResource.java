package br.com.projeto.rest;

import br.com.projeto.annotations.RequireRole;
import br.com.projeto.service.AuthService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/{id}/promote")
    @Transactional
    @RequireRole("admin")
    public Response promote(@PathParam("id") String userId){
        boolean ok = authService.promoteToManager(userId);
        if(!ok) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }
}
