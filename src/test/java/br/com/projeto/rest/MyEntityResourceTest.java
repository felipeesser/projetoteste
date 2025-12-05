package br.com.projeto.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.projeto.models.MyEntity;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class MyEntityResourceTest {

    @Inject
    EntityManager em;

    @BeforeEach
    @Transactional
    public void cleanDatabase() {
        em.createQuery("DELETE FROM MyEntity").executeUpdate();
    }

    @Test
    public void testCreateSuccess() {
        MyEntity entity = new MyEntity();
        entity.nome = "Test Name";

        given()
            .contentType("application/json")
            .body(entity)
        .when()
            .post("/myentity")
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .body("nome", equalTo("Test Name"));
    }

    @Test
    public void testCreateBadRequest() {
        MyEntity entity = new MyEntity();
        entity.nome = null;

        given()
            .contentType("application/json")
            .body(entity)
        .when()
            .post("/myentity")
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
            .body("statusCode", equalTo(Response.Status.BAD_REQUEST.getStatusCode()))
            .body("message", equalTo("Nome é obrigatório"));
    }

    @Test
    public void testListEntities() {
        MyEntity entity = new MyEntity();
        entity.nome = "Entity 1";

        given()
            .contentType("application/json")
            .body(entity)
        .when()
            .post("/myentity")
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .body("nome", equalTo("Entity 1"));
        
        MyEntity entity2 = new MyEntity();
        entity2.nome = "Entity 2";

        given()
            .contentType("application/json")
            .body(entity2)
        .when()
            .post("/myentity")
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .body("nome", equalTo("Entity 2"));


        given()
        .when()
            .get("/myentity/listar")
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("", hasSize(2))
            .body("[0].nome", equalTo("Entity 1"))
            .body("[1].nome", equalTo("Entity 2"));
    }
}