package br.com.projeto.rest;

import java.util.List;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import br.com.projeto.annotations.RequireRole;
import br.com.projeto.models.Employee;
import br.com.projeto.models.EmployeeDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.projeto.service.EmployeeService;
import io.quarkus.runtime.annotations.RegisterForReflection;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;

@Path("/api/employees")
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeResource {

    @Inject
    EmployeeService employeeService;
    @Inject
    ObjectMapper objectMapper;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @RequireRole({"gestor", "admin"})
    public Response create(@RestForm String userId, 
                          @RestForm String data,
                          @RestForm String managerId,
                          @RestForm("files") List<FileUpload> files){
        if(userId == null || userId.trim().isEmpty()){
            return Response.status(Response.Status.BAD_REQUEST).entity("userId is required").build();
        }
        
        Employee e;
        if(managerId != null && !managerId.trim().isEmpty()){
            // Prevenir criação de funcionário que seja gerente de si mesmo
            if(userId != null && managerId.trim().equals(userId.trim())){
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Não é possível atribuir o próprio usuário como gestor\"}").build();
            }
            e = employeeService.createEmployeeForUserWithManager(userId, data, managerId);
        } else {
            e = employeeService.createEmployeeForUser(userId, data);
        }
        
        // Adicionar documentos com arquivos
        if(files != null && !files.isEmpty()) {
            for(FileUpload file : files) {
                try {
                    byte[] fileData = java.nio.file.Files.readAllBytes(file.filePath());
                    employeeService.addDocument(e.id.toString(), file.fileName(), file.fileName(), 
                                               file.contentType(), fileData);
                } catch(Exception ex) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Erro ao processar arquivo: " + file.fileName()).build();
                }
            }
        }
        
        return Response.status(Response.Status.CREATED).entity(toDto(e)).build();
    }

    @POST
    @Path("/self-register")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @RequireRole("funcionario")
    public Response selfRegister(@RestForm String data,
                                 @RestForm("files") List<FileUpload> files,
                                 @Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        
        Employee e = employeeService.createEmployeeForUser(userId, data);
        
        // Adicionar documentos com arquivos
        if(files != null && !files.isEmpty()) {
            for(FileUpload file : files) {
                try {
                    byte[] fileData = java.nio.file.Files.readAllBytes(file.filePath());
                    employeeService.addDocument(e.id.toString(), file.fileName(), file.fileName(), 
                                               file.contentType(), fileData);
                } catch(Exception ex) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing file: " + file.fileName()).build();
                }
            }
        }
        
        return Response.status(Response.Status.CREATED).entity(toDto(e)).build();
    }

    @POST
    @Path("/{employeeId}/documents")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @RequireRole({"funcionario", "gestor", "admin"})
    public Response addDocument(@PathParam("employeeId") String employeeId,
                               @RestForm String documentName,
                               @RestForm("file") FileUpload file,
                               @Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");
        
        // Funcionário só pode adicionar documentos ao seu próprio cadastro
        if("funcionario".equals(userRole)) {
            var employee = employeeService.getById(employeeId);
            if(employee.isEmpty() || !employee.get().userId.toString().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Acesso negado\"}").build();
            }
        }
        
        try {
            byte[] fileData = java.nio.file.Files.readAllBytes(file.filePath());
            employeeService.addDocument(employeeId, documentName, file.fileName(), 
                                       file.contentType(), fileData);
            return Response.ok().build();
        } catch(Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erro ao processar arquivo").build();
        }
    }

    @POST
    @Path("/{employeeId}/documents/{documentName}/update")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @RequireRole({"funcionario", "gestor", "admin"})
    public Response updateDocument(@PathParam("employeeId") String employeeId,
                                  @PathParam("documentName") String documentName,
                                  @RestForm("file") FileUpload file,
                                  @Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");
        
        // Funcionário só pode atualizar seus próprios documentos
        if("funcionario".equals(userRole)) {
            var employee = employeeService.getById(employeeId);
            if(employee.isEmpty() || !employee.get().userId.toString().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Acesso negado\"}").build();
            }
        }
        
        try {
            byte[] fileData = java.nio.file.Files.readAllBytes(file.filePath());
            boolean updated = employeeService.updateDocument(employeeId, documentName, file.fileName(), 
                                                            file.contentType(), fileData);
            if(!updated) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch(Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erro ao processar arquivo").build();
        }
    }

    @GET
    @Path("/{employeeId}/documents/{documentName}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RequireRole({"funcionario", "gestor", "admin"})
    public Response downloadDocument(@PathParam("employeeId") String employeeId,
                                    @PathParam("documentName") String documentName,
                                    @Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");
        
        // Funcionário só pode baixar seus próprios documentos
        if("funcionario".equals(userRole)) {
            var employee = employeeService.getById(employeeId);
            if(employee.isEmpty() || !employee.get().userId.toString().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Acesso negado\"}").build();
            }
        }
        
        var docOpt = employeeService.getDocument(employeeId, documentName);
        if(docOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        EmployeeDocument doc = docOpt.get();
        return Response.ok(doc.fileData)
            .header("Content-Type", doc.contentType)
            .header("Content-Disposition", "attachment; filename=\"" + doc.fileName + "\"")
            .build();
    }

    @GET
    @Path("/by-user/{userId}")
    @RequireRole({"funcionario", "gestor", "admin"})
    public Response getByUserId(@PathParam("userId") String userId, @Context ContainerRequestContext requestContext){
        String tokenUserId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");
        
        // Funcionário só pode acessar seus próprios dados
        if("funcionario".equals(userRole) && !userId.equals(tokenUserId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Acesso negado\"}").build();
        }
        
        var employee = employeeService.getByUserId(userId);
        if(employee.isEmpty()){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toDto(employee.get())).build();
    }

    @POST
    @Path("/{employeeId}/documents/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RequireRole({"gestor", "admin"})
    public Response approve(@PathParam("employeeId") String employeeId, ApproveReq req){
        boolean ok = employeeService.approveDocument(employeeId, req.name, req.approved);
        if(!ok) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    @RegisterForReflection
    public static class AssignManagerReq { public String managerId; }

    @POST
    @Path("/{employeeId}/assign-manager")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RequireRole({"gestor", "admin"})
    public Response assignManager(@PathParam("employeeId") String employeeId, AssignManagerReq req, @Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");

        var empOpt = employeeService.getById(employeeId);

        // Gestor só pode atribuir gerente a si mesmo, mas pode desatribuir funcionários que gerencia atualmente
        if ("gestor".equals(userRole)) {
            // Funcionário deve existir para verificar seu gerente atual
            if (empOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            var employee = empOpt.get();

            if (req.managerId == null) {
                // Permitir desatribuição apenas se o gerente atual for este gestor
                if (employee.managerId == null || !employee.managerId.toString().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Gestor só pode remover vínculo de seus próprios funcionários\"}").build();
                }
            } else {
                // Atribuição: gestor só pode atribuir o funcionário a si mesmo
                if (!req.managerId.equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Gestor só pode vincular funcionários a si mesmo\"}").build();
                }
            }
        }

        // Prevenir atribuição de funcionário como gerente de si mesmo
        if(empOpt.isPresent() && req.managerId != null && req.managerId.equals(empOpt.get().userId.toString())){
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Não é possível atribuir o próprio usuário como gestor\"}").build();
        }

        boolean ok = employeeService.assignManager(employeeId, req.managerId);
        if(!ok) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    @GET
    @RequireRole({"gestor", "admin"})
    public Response listEmployees(@Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");
        
        List<Employee> employees;
        
        if("admin".equals(userRole)) {
            // Admin vê todos
            employees = employeeService.getAllEmployees();
        } else {
            // Gestor vê apenas seus funcionários
            employees = employeeService.getEmployeesByManager(userId);
        }
        
        List<EmployeeDto> dtos = employees.stream().map(this::toDto).toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/by-manager/{managerId}")
    @RequireRole({"gestor", "admin"})
    public Response listEmployeesByManager(@PathParam("managerId") String managerId, @Context ContainerRequestContext requestContext){
        String userId = (String) requestContext.getProperty("userId");
        String userRole = (String) requestContext.getProperty("userRole");
        
        // Gestor só pode ver seus próprios funcionários
        if("gestor".equals(userRole) && !managerId.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Acesso negado\"}").build();
        }
        
        List<Employee> employees = employeeService.getEmployeesByManager(managerId);
        List<EmployeeDto> dtos = employees.stream().map(this::toDto).toList();
        return Response.ok(dtos).build();
    }

    // DTOs
    @RegisterForReflection
    public static class ApproveReq { public String name; public boolean approved; }

    @RegisterForReflection
    public static class EmployeeDto{
        public String id;
        public String userId;
        public Object data;
        public java.util.List<DocumentDto> documents;
        public String managerId;
    }
    
    @RegisterForReflection
    public static class DocumentDto{
        public String name; 
        public Boolean approved;
        public String fileName;
        public String contentType;
        public Long fileSize;
        
        public DocumentDto(String name, Boolean approved, String fileName, String contentType, Long fileSize){ 
            this.name=name; 
            this.approved=approved; 
            this.fileName=fileName;
            this.contentType=contentType;
            this.fileSize=fileSize;
        }
    }

    private EmployeeDto toDto(Employee e){
        EmployeeDto dto = new EmployeeDto();
        dto.id = e.id.toString();
        dto.userId = e.userId.toString();
        dto.managerId = e.managerId == null ? null : e.managerId.toString();
        dto.data = e.data;
        dto.documents = new java.util.ArrayList<>();
        for(var d : e.documents){
            dto.documents.add(new DocumentDto(d.name, d.approved, d.fileName, d.contentType, d.fileSize));
        }
        return dto;
    }
}
