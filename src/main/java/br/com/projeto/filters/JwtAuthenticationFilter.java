package br.com.projeto.filters;

import java.io.IOException;

import br.com.projeto.utils.JwtUtil;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    @Inject
    JwtUtil jwtUtil;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        
        // Normalizar path (remover barra inicial se existir)
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // Permitir apenas rotas de autenticação específicas (públicas)
        if(path.equals("api/auth/has-admin") || 
           path.equals("api/auth/login") || 
           path.equals("api/auth/register") || 
           path.equals("api/auth/admin")) {
            return;
        }
        
        // Extrair token do header Authorization
        String authHeader = requestContext.getHeaderString("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token não fornecido\"}")
                    .build()
            );
            return;
        }
        
        String token = authHeader.substring(7);
        JwtUtil.TokenPayload payload = jwtUtil.validateToken(token);
        
        if(payload == null) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token inválido ou expirado\"}")
                    .build()
            );
            return;
        }
        
        // Adicionar informações do usuário ao contexto da requisição
        requestContext.setProperty("userId", payload.id);
        requestContext.setProperty("username", payload.username);
        requestContext.setProperty("userRole", payload.role);
    }
}
