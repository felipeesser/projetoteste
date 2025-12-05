package br.com.projeto.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.io.File;
import java.io.FileOutputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeeResourceTest {

    private static String adminToken;
    private static String adminUserId;
    private static String managerToken;
    private static String managerUserId;
    private static String employeeToken;
    private static String employeeUserId;
    private static String employeeId;
    private static File testFile;

    @Test
    @Order(1)
    public void testSetup() throws Exception {
        // Verificar se admin existe, se não criar
        var hasAdmin = given()
            .when().get("/api/auth/has-admin")
            .then()
                .statusCode(200)
                .extract().path("hasAdmin");
        
        if (!(Boolean)hasAdmin) {
            String adminBody = "{\"username\":\"empAdmin\",\"password\":\"admin123\"}";
            var adminResp = given()
                .contentType(ContentType.JSON)
                .body(adminBody)
                .when().post("/api/auth/admin")
                .then()
                    .statusCode(201)
                    .extract();
            
            adminToken = adminResp.path("token");
            adminUserId = adminResp.path("user.id");
        } else {
            // Login como admin existente
            String adminBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
            var adminResp = given()
                .contentType(ContentType.JSON)
                .body(adminBody)
                .when().post("/api/auth/login")
                .then()
                    .statusCode(200)
                    .extract();
            
            adminToken = adminResp.path("token");
            adminUserId = adminResp.path("user.id");
        }

        // Criar e promover gestor com nome único
        String managerUsername = "empManager" + System.currentTimeMillis();
        String managerBody = "{\"username\":\"" + managerUsername + "\",\"password\":\"manager123\"}";
        var managerResp = given()
            .contentType(ContentType.JSON)
            .body(managerBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract();
        
        managerUserId = managerResp.path("user.id");
        
        // Promover para gestor
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().post("/api/users/" + managerUserId + "/promote")
            .then()
                .statusCode(200);

        // Fazer login novamente para obter novo token com role atualizada
        String managerLoginBody = "{\"username\":\"" + managerUsername + "\",\"password\":\"manager123\"}";
        managerToken = given()
            .contentType(ContentType.JSON)
            .body(managerLoginBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(200)
                .extract().path("token");

        // Criar usuário funcionário com nome único
        String employeeBody = "{\"username\":\"empEmployee" + System.currentTimeMillis() + "\",\"password\":\"emp123\"}";
        var employeeResp = given()
            .contentType(ContentType.JSON)
            .body(employeeBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract();
        
        employeeToken = employeeResp.path("token");
        employeeUserId = employeeResp.path("user.id");

        // Criar arquivo de teste
        testFile = File.createTempFile("test", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("Test PDF content".getBytes());
        }
    }

    @Test
    @Order(2)
    public void testCreateEmployeeAsManager() {
        String jsonData = "{\"name\":\"João Silva\",\"cpf\":\"123.456.789-00\"}";
        
        employeeId = given()
            .header("Authorization", "Bearer " + managerToken)
            .multiPart("userId", employeeUserId)
            .multiPart("data", jsonData)
            .multiPart("managerId", managerUserId)
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", is(employeeUserId))
                .body("managerId", is(managerUserId))
                .extract().path("id");
    }

    @Test
    @Order(3)
    public void testCreateEmployeeAsAdmin() {
        String jsonData = "{\"name\":\"Maria Santos\",\"cpf\":\"987.654.321-00\"}";
        
        // Criar outro usuário para este funcionário com nome único
        String userBody = "{\"username\":\"anotherEmp" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        String newUserId = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("user.id");

        given()
            .header("Authorization", "Bearer " + adminToken)
            .multiPart("userId", newUserId)
            .multiPart("data", jsonData)
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", is(newUserId));
    }

    @Test
    @Order(4)
    public void testCreateEmployeeWithoutToken() {
        String jsonData = "{\"name\":\"Test\"}";
        
        given()
            .multiPart("userId", employeeUserId)
            .multiPart("data", jsonData)
            .when().post("/api/employees")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    public void testCreateEmployeeAsRegularEmployee() {
        String jsonData = "{\"name\":\"Test\"}";
        
        given()
            .header("Authorization", "Bearer " + employeeToken)
            .multiPart("userId", employeeUserId)
            .multiPart("data", jsonData)
            .when().post("/api/employees")
            .then()
                .statusCode(403);
    }

    @Test
    @Order(6)
    public void testSelfRegisterAsEmployee() {
        // Criar novo usuário funcionário com nome único
        String userBody = "{\"username\":\"selfRegEmp" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        var resp = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract();
        
        String token = resp.path("token");
        String selfUserId = resp.path("user.id");
        String jsonData = "{\"name\":\"Self Registered\",\"cpf\":\"111.222.333-44\"}";

        given()
            .header("Authorization", "Bearer " + token)
            .multiPart("data", jsonData)
            .when().post("/api/employees/self-register")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", is(selfUserId))
                .body("managerId", nullValue());
    }

    @Test
    @Order(7)
    public void testAddDocumentToEmployee() throws Exception {
        given()
            .header("Authorization", "Bearer " + managerToken)
            .multiPart("documentName", "RG")
            .multiPart("file", testFile, "application/pdf")
            .when().post("/api/employees/" + employeeId + "/documents")
            .then()
                .statusCode(200);

        // Verificar que documento foi adicionado
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-user/" + employeeUserId)
            .then()
                .statusCode(200)
                .body("documents.size()", is(1))
                .body("documents[0].name", is("RG"))
                .body("documents[0].fileName", is(testFile.getName()))
                .body("documents[0].approved", nullValue());
    }

    @Test
    @Order(8)
    public void testUpdateDocument() throws Exception {
        File updatedFile = File.createTempFile("updated", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(updatedFile)) {
            fos.write("Updated PDF content".getBytes());
        }

        given()
            .header("Authorization", "Bearer " + managerToken)
            .multiPart("file", updatedFile, "application/pdf")
            .when().post("/api/employees/" + employeeId + "/documents/RG/update")
            .then()
                .statusCode(200);

        // Verificar que aprovação foi resetada
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-user/" + employeeUserId)
            .then()
                .statusCode(200)
                .body("documents[0].approved", nullValue());

        updatedFile.delete();
    }

    @Test
    @Order(9)
    public void testDownloadDocument() {
        byte[] downloadedData = given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/" + employeeId + "/documents/RG/download")
            .then()
                .statusCode(200)
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", containsString("attachment"))
                .extract().asByteArray();

        assert downloadedData.length > 0;
    }

    @Test
    @Order(10)
    public void testEmployeeCanOnlyAccessOwnData() {
        given()
            .header("Authorization", "Bearer " + employeeToken)
            .when().get("/api/employees/by-user/" + employeeUserId)
            .then()
                .statusCode(200)
                .body("userId", is(employeeUserId));

        // Tentar acessar dados de outro usuário (deve falhar)
        given()
            .header("Authorization", "Bearer " + employeeToken)
            .when().get("/api/employees/by-user/" + adminUserId)
            .then()
                .statusCode(403);
    }

    @Test
    @Order(11)
    public void testEmployeeCanOnlyAccessOwnDocuments() {
        // Funcionário tenta acessar seu próprio documento (deve ter sucesso)
        given()
            .header("Authorization", "Bearer " + employeeToken)
            .when().get("/api/employees/" + employeeId + "/documents/RG/download")
            .then()
                .statusCode(200);
    }

    @Test
    @Order(12)
    public void testApproveDocumentAsManager() {
        String approveBody = "{\"name\":\"RG\",\"approved\":true}";
        
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(approveBody)
            .when().post("/api/employees/" + employeeId + "/documents/approve")
            .then()
                .statusCode(200);

        // Verificar que documento foi aprovado
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-user/" + employeeUserId)
            .then()
                .statusCode(200)
                .body("documents[0].approved", is(true));
    }

    @Test
    @Order(13)
    public void testRejectDocument() {
        String rejectBody = "{\"name\":\"RG\",\"approved\":false}";
        
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(rejectBody)
            .when().post("/api/employees/" + employeeId + "/documents/approve")
            .then()
                .statusCode(200);

        // Verificar que documento foi rejeitado
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-user/" + employeeUserId)
            .then()
                .statusCode(200)
                .body("documents[0].approved", is(false));
    }

    @Test
    @Order(14)
    public void testListAllEmployeesAsAdmin() {
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().get("/api/employees")
            .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @Order(15)
    public void testListEmployeesByManager() {
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees")
            .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].managerId", is(managerUserId));
    }

    @Test
    @Order(16)
    public void testListEmployeesBySpecificManager() {
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().get("/api/employees/by-manager/" + managerUserId)
            .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @Order(17)
    public void testManagerCanOnlyListOwnEmployees() {
        // Gestor tenta listar funcionários de outro gestor (deve falhar)
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-manager/" + adminUserId)
            .then()
                .statusCode(403);

        // Gestor lista seus próprios funcionários (deve ter sucesso)
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-manager/" + managerUserId)
            .then()
                .statusCode(200);
    }

    @Test
    @Order(18)
    public void testGetByUserIdNotFound() {
        String fakeUserId = "550e8400-e29b-41d4-a716-446655440000";
        
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().get("/api/employees/by-user/" + fakeUserId)
            .then()
                .statusCode(404);
    }

    @Test
    @Order(19)
    public void testApproveNonexistentDocument() {
        String approveBody = "{\"name\":\"NONEXISTENT\",\"approved\":true}";
        
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(approveBody)
            .when().post("/api/employees/" + employeeId + "/documents/approve")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(20)
    public void testCreateEmployeeWithFiles() throws Exception {
        String jsonData = "{\"name\":\"Employee With Files\",\"cpf\":\"999.888.777-66\"}";
        
        // Criar usuário com nome único
        String userBody = "{\"username\":\"empWithFiles" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        String newUserId = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("user.id");

        File file1 = File.createTempFile("doc1", ".pdf");
        File file2 = File.createTempFile("doc2", ".jpg");
        
        try (FileOutputStream fos1 = new FileOutputStream(file1);
             FileOutputStream fos2 = new FileOutputStream(file2)) {
            fos1.write("Document 1 content".getBytes());
            fos2.write("Document 2 content".getBytes());
        }

        given()
            .header("Authorization", "Bearer " + managerToken)
            .multiPart("userId", newUserId)
            .multiPart("data", jsonData)
            .multiPart("managerId", managerUserId)
            .multiPart("files", file1, "application/pdf")
            .multiPart("files", file2, "image/jpeg")
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .body("documents.size()", is(2));

        file1.delete();
        file2.delete();
    }

    @Test
    @Order(21)
    public void testAssignManagerAsAdmin() {
        // Criar usuário para novo funcionário
        String userBody = "{\"username\":\"empAssignAdmin" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        String newUserId = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("user.id");

        // Admin cria funcionário sem gestor
        String jsonData = "{\"name\":\"Assign Test\"}";
        String newEmployeeId = given()
            .header("Authorization", "Bearer " + adminToken)
            .multiPart("userId", newUserId)
            .multiPart("data", jsonData)
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");

        // Admin atribui gestor a este funcionário
        String assignBody = "{\"managerId\":\"" + managerUserId + "\"}";
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body(assignBody)
            .when().post("/api/employees/" + newEmployeeId + "/assign-manager")
            .then()
                .statusCode(200);

        // Verificar atribuição
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().get("/api/employees/by-user/" + newUserId)
            .then()
                .statusCode(200)
                .body("managerId", is(managerUserId));
    }

    @Test
    @Order(22)
    public void testManagerCanAssignSelf() {
        // Criar usuário e funcionário com admin
        String userBody = "{\"username\":\"empAssignManagerSelf" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        String newUserId = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("user.id");

        String jsonData = "{\"name\":\"Assign Self\"}";
        String newEmployeeId = given()
            .header("Authorization", "Bearer " + adminToken)
            .multiPart("userId", newUserId)
            .multiPart("data", jsonData)
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Gestor atribui este novo funcionário a si mesmo
        String assignBody = "{\"managerId\":\"" + managerUserId + "\"}";
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(assignBody)
            .when().post("/api/employees/" + newEmployeeId + "/assign-manager")
            .then()
                .statusCode(200);

        // Verificar atribuição
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-user/" + newUserId)
            .then()
                .statusCode(200)
                .body("managerId", is(managerUserId));
    }

    @Test
    @Order(23)
    public void testManagerCannotAssignAnotherManager() {
        // Tentar atribuir um funcionário a outro gestor (admin) como gestor --> deve ser proibido
        String assignBody = "{\"managerId\":\"" + adminUserId + "\"}";
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(assignBody)
            .when().post("/api/employees/" + employeeId + "/assign-manager")
            .then()
                .statusCode(403);
    }

    @Test
    @Order(24)
    public void testManagerCannotCreateEmployeeWithSelfAsManager() {
        String jsonData = "{\"name\":\"Self Manager Create\"}";
        // Gestor tenta criar funcionário usando seu próprio userId e definindo managerId para seu próprio id
        given()
            .header("Authorization", "Bearer " + managerToken)
            .multiPart("userId", managerUserId)
            .multiPart("data", jsonData)
            .multiPart("managerId", managerUserId)
            .when().post("/api/employees")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(25)
    public void testAssignManagerRejectsSelfAssignment() {
        // Criar registro de funcionário cujo user id é igual a managerUserId (admin cria)
        String jsonData = "{\"name\":\"Manager Self Employee\"}";
        String empId = given()
            .header("Authorization", "Bearer " + adminToken)
            .multiPart("userId", managerUserId)
            .multiPart("data", jsonData)
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Admin tenta atribuir managerId igual a employee.userId (auto-atribuição) -> deve ser rejeitado
        String assignBody = "{\"managerId\":\"" + managerUserId + "\"}";
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body(assignBody)
            .when().post("/api/employees/" + empId + "/assign-manager")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(26)
    public void testManagerCanUnassignOwnEmployee() {
        // Gestor desatribui o funcionário criado na ordem 2
        String assignBody = "{\"managerId\":null}";
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(assignBody)
            .when().post("/api/employees/" + employeeId + "/assign-manager")
            .then()
                .statusCode(200);

        // Verificar que managerId é null
        given()
            .header("Authorization", "Bearer " + managerToken)
            .when().get("/api/employees/by-user/" + employeeUserId)
            .then()
                .statusCode(200)
                .body("managerId", nullValue());
    }

    @Test
    @Order(27)
    public void testManagerCannotUnassignEmployeeManagedByAnother() {
        // Criar usuário e funcionário atribuído ao admin
        String userBody = "{\"username\":\"empUnassignOther" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        String newUserId = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("user.id");

        String jsonData = "{\"name\":\"Assigned to Admin\"}";
        String newEmployeeId = given()
            .header("Authorization", "Bearer " + adminToken)
            .multiPart("userId", newUserId)
            .multiPart("data", jsonData)
            .multiPart("managerId", adminUserId)
            .when().post("/api/employees")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Gestor tenta desatribuir este funcionário (deve ser proibido)
        String assignBody = "{\"managerId\":null}";
        given()
            .header("Authorization", "Bearer " + managerToken)
            .contentType(ContentType.JSON)
            .body(assignBody)
            .when().post("/api/employees/" + newEmployeeId + "/assign-manager")
            .then()
                .statusCode(403);
    }
}
