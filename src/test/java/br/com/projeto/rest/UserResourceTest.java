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
public class UserResourceTest {

    private static String adminToken;
    private static String userId;
    private static String username;

    @Test
    @Order(1)
    public void testSetup() {
        // Verificar se admin existe, se não criar
        var hasAdmin = given()
            .when().get("/api/auth/has-admin")
            .then()
                .statusCode(200)
                .extract().path("hasAdmin");
        
        if (!(Boolean)hasAdmin) {
            String adminBody = "{\"username\":\"useradmin\",\"password\":\"admin123\"}";
            adminToken = given()
                .contentType(ContentType.JSON)
                .body(adminBody)
                .when().post("/api/auth/admin")
                .then()
                    .statusCode(201)
                    .extract().path("token");
        } else {
            // Login como admin existente
            String adminBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
            adminToken = given()
                .contentType(ContentType.JSON)
                .body(adminBody)
                .when().post("/api/auth/login")
                .then()
                    .statusCode(200)
                    .extract().path("token");
        }

        // Criar usuário regular com nome único
        username = "usertest" + System.currentTimeMillis();
        String userBody = "{\"username\":\"" + username + "\",\"password\":\"user123\"}";
        userId = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("user.id");
    }

    @Test
    @Order(2)
    public void testPromoteUserToManagerAsAdmin() {
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().post("/api/users/" + userId + "/promote")
            .then()
                .statusCode(200);

        // Verificar se usuário agora é gestor
        String loginBody = "{\"username\":\"" + username + "\",\"password\":\"user123\"}";
        given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .when().post("/api/auth/login")
            .then()
                .statusCode(200)
                .body("user.role", is("gestor"));
    }

    @Test
    @Order(3)
    public void testPromoteUserWithoutToken() {
        given()
            .when().post("/api/users/" + userId + "/promote")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(4)
    public void testPromoteUserAsNonAdmin() {
        // Criar outro usuário para testar
        String userBody = "{\"username\":\"regularuser" + System.currentTimeMillis() + "\",\"password\":\"pass123\"}";
        String regularToken = given()
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/auth/register")
            .then()
                .statusCode(201)
                .extract().path("token");

        // Tentar promover usando token não-admin
        given()
            .header("Authorization", "Bearer " + regularToken)
            .when().post("/api/users/" + userId + "/promote")
            .then()
                .statusCode(403);
    }

    @Test
    @Order(5)
    public void testPromoteNonexistentUser() {
        String fakeUserId = "550e8400-e29b-41d4-a716-446655440000";
        
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().post("/api/users/" + fakeUserId + "/promote")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    public void testPromoteWithInvalidUserId() {
        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().post("/api/users/invalid-uuid/promote")
            .then()
                .statusCode(404);
    }
}
