package br.com.projeto.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthResourceTest {

    @Test
    @Order(1)
    public void testHasAdminInitiallyFalse() {
        given()
            .when().get("/api/auth/has-admin")
            .then()
                .statusCode(200)
                .body("hasAdmin", is(false));
    }

    @Test
    @Order(2)
    public void testCreateAdmin() {
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/admin")
            .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("user.username", is("admin"))
                .body("user.role", is("admin"))
                .body("user.id", notNullValue());
    }

    @Test
    @Order(3)
    public void testHasAdminAfterCreation() {
        given()
            .when().get("/api/auth/has-admin")
            .then()
                .statusCode(200)
                .body("hasAdmin", is(true));
    }

    @Test
    @Order(4)
    public void testCreateAdminTwiceFails() {
        String requestBody = "{\"username\":\"admin2\",\"password\":\"admin123\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/admin")
            .then()
                .statusCode(400)
                .body("error", is("Admin já existe ou dados inválidos"));
    }

    @Test
    @Order(5)
    public void testRegisterNewUser() {
        String requestBody = "{\"username\":\"user1\",\"password\":\"pass123\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("user.username", is("user1"))
                .body("user.role", is("funcionario"))
                .body("user.id", notNullValue());
    }

    @Test
    @Order(6)
    public void testRegisterDuplicateUserFails() {
        String requestBody = "{\"username\":\"user1\",\"password\":\"pass456\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(400)
                .body("error", is("Usuário já existe ou inválido"));
    }

    @Test
    @Order(7)
    public void testLoginWithValidCredentials() {
        String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.username", is("admin"))
                .body("user.role", is("admin"));
    }

    @Test
    @Order(8)
    public void testLoginWithInvalidPassword() {
        String requestBody = "{\"username\":\"admin\",\"password\":\"wrongpassword\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(401)
                .body("error", is("Credenciais inválidas"));
    }

    @Test
    @Order(9)
    public void testLoginWithNonexistentUser() {
        String requestBody = "{\"username\":\"nonexistent\",\"password\":\"pass123\"}";
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(401)
                .body("error", is("Credenciais inválidas"));
    }

    @Test
    @Order(10)
    public void testGetAllUsersAsAdmin() {
        // Primeiro fazer login como admin
        String loginBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        String token = given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(200)
                .extract().path("token");

        // Depois pegar todos os usuários
        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/auth/users")
            .then()
                .statusCode(200)
                .body("size()", is(2)) // admin e user1
                .body("[0].username", notNullValue())
                .body("[0].role", notNullValue());
    }

    @Test
    @Order(11)
    public void testGetAllUsersWithoutToken() {
        given()
            .when().get("/api/auth/users")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(12)
    public void testGetAllUsersAsNonAdmin() {
        // Login como usuário regular
        String loginBody = "{\"username\":\"user1\",\"password\":\"pass123\"}";
        String token = given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(200)
                .extract().path("token");

        // Tentar pegar todos os usuários (deve falhar - não é admin)
        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/auth/users")
            .then()
                .statusCode(403);
    }
}
