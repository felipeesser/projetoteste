package br.com.projeto.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * Resource para suportar SPA routing do Angular.
 * Serve index.html para todas as rotas que não sejam /api/*
 */
@Path("/")
public class SpaResource {

    @GET
    @Path("{path:.*}")
    @Produces(MediaType.TEXT_HTML)
    public InputStream serveSpa(@PathParam("path") String path) {
        // Se for uma rota de API, não intercepta (já tratado por outros Resources)
        if (path != null && path.startsWith("api/")) {
            return null;
        }
        
        // Serve index.html para permitir que Angular gerencie roteamento
        InputStream indexHtml = getClass().getClassLoader()
            .getResourceAsStream("META-INF/resources/index.html");
        
        return indexHtml;
    }
}
