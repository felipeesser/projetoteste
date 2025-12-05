package br.com.projeto.filters;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import br.com.projeto.annotations.RequireRole;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RoleAuthorizationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        
        if(method == null) return;
        
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if(requireRole == null) {
            requireRole = resourceInfo.getResourceClass().getAnnotation(RequireRole.class);
        }
        
        if(requireRole == null) return;
        
        String userRole = (String) requestContext.getProperty("userRole");
        String[] requiredRoles = requireRole.value();
        
        System.out.println("🔐 Endpoint: " + requestContext.getUriInfo().getPath());
        System.out.println("🔐 User role: " + userRole);
        System.out.println("🔐 Required roles: " + Arrays.toString(requiredRoles));
        
        if(userRole == null || !Arrays.asList(requireRole.value()).contains(userRole)) {
            System.out.println("❌ Acesso negado!");
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Acesso negado. Permissão insuficiente. Requer: " + Arrays.toString(requiredRoles) + ", Possui: " + userRole + "\"}")
                    .build()
            );
        } else {
            System.out.println("✅ Acesso permitido!");
        }
    }
}
