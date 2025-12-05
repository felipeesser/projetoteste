package br.com.projeto.rest;

import br.com.projeto.annotations.RequireRole;
import br.com.projeto.service.AuthService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @GET
    @Path("/has-admin")
    public Response hasAdmin(){
        boolean has = authService.hasAdmin();
        return Response.ok(new HasAdmin(has)).build();
    }

    @POST
    @Path("/admin")
    @Transactional
    public Response createAdmin(Credentials c){
        var res = authService.createAdmin(c.username, c.password);
        if(res == null){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMsg("Admin já existe ou dados inválidos")).build();
        }
        return Response.status(Response.Status.CREATED).entity(res).build();
    }

    @POST
    @Path("/register")
    @Transactional
    public Response register(Credentials c){
        var res = authService.register(c.username, c.password);
        if(res == null){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMsg("Usuário já existe ou inválido")).build();
        }
        return Response.status(Response.Status.CREATED).entity(res).build();
    }

    @POST
    @Path("/login")
    @Transactional
    public Response login(Credentials c){
        var res = authService.login(c.username, c.password);
        if(res == null){
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorMsg("Credenciais inválidas")).build();
        }
        return Response.ok(res).build();
    }

    @GET
    @Path("/users")
    @RequireRole("admin")
    public Response getAllUsers(){
        var users = authService.getAllUsers();
        return Response.ok(users).build();
    }

    @RegisterForReflection
    public static class Credentials{
        public String username;
        public String password;
    }
    
    @RegisterForReflection
    public static class HasAdmin{ 
        public boolean hasAdmin; 
        public HasAdmin(boolean b){ this.hasAdmin=b; } 
    }
    
    @RegisterForReflection
    public static class ErrorMsg{ 
        public String error; 
        public ErrorMsg(String e){ this.error=e; } 
    }
}
