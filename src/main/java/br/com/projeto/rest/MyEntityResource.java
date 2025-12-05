package br.com.projeto.rest;

import java.util.List;
import java.util.UUID;

import br.com.projeto.models.MyEntity;
import br.com.projeto.rest.errors.ErrorResponse;
import br.com.projeto.service.MyEntityService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/myentity")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MyEntityResource{

    @Inject
    MyEntityService myEntityService;

    @POST
    @Transactional
    public Response create(MyEntity entity) {
        if (entity.nome == null) {
            ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "Nome é obrigatório");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }
        return Response.status(Response.Status.CREATED).entity(myEntityService.createEntity(entity)).build();
    }

    @GET
    @Path("/{uuid}")
    public Response get(@PathParam("uuid") UUID uuid) {
        MyEntity entity = myEntityService.getEntity(uuid);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/listar")
    public Response listar() {
        List<MyEntity> allEntities = myEntityService.getAll();
        
        return Response.ok(allEntities).build();
    }
}