package com.clientnotes.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EngagementNoteResourceTest {

    private static Long createdBlockerId;

    @Test
    @Order(1)
    void requirement1_shouldCreateEngagementNote_andRejectBlankFields() {
        // GIVEN a valid client engagement note payload
        String validPayload = """
            {
              "clientCode": "ACME-UK",
              "author": "Begum",
              "status": "BLOCKER",
              "summary": "Client firewall blocks outbound port 8443; awaiting network ticket #412"
            }
            """;

        // WHEN POST /api/v1/notes is called
        Integer id = given()
                .contentType(ContentType.JSON)
                .body(validPayload)
            .when()
                .post("/api/v1/notes")
            .then()
                // THEN it persists in PostgreSQL and returns 201 Created
                .statusCode(201)
                .body("clientCode", equalTo("ACME-UK"))
                .body("status", equalTo("BLOCKER"))
                .body("updatedAt", notNullValue())
                .extract()
                .path("id");

        createdBlockerId = id.longValue();

        // GIVEN an invalid payload missing summary
        String invalidPayload = """
            {
              "clientCode": "ACME-UK",
              "author": "Begum",
              "status": "IN_PROGRESS",
              "summary": ""
            }
            """;

        // WHEN POST /api/v1/notes is called -> THEN 400 Bad Request
        given()
            .contentType(ContentType.JSON)
            .body(invalidPayload)
        .when()
            .post("/api/v1/notes")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(2)
    void requirement2_shouldRetrieveLatestNotesAndFilterByStatus() {
        // GIVEN a second note is logged for ACME-UK
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "clientCode": "ACME-UK",
                  "author": "Alex",
                  "status": "WORKAROUND",
                  "summary": "Configured SSH tunnel over port 443 as temporary workaround"
                }
                """)
        .when()
            .post("/api/v1/notes")
        .then()
            .statusCode(201);

        // WHEN GET /api/v1/notes/ACME-UK is called without filter
        given()
        .when()
            .get("/api/v1/notes/ACME-UK")
        .then()
            // THEN both notes are returned ordered by most recent first
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].status", equalTo("WORKAROUND"))
            .body("[1].status", equalTo("BLOCKER"));

        // WHEN GET /api/v1/notes/ACME-UK?status=BLOCKER is called
        given()
            .queryParam("status", "BLOCKER")
        .when()
            .get("/api/v1/notes/ACME-UK")
        .then()
            // THEN only the active BLOCKER note is returned
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].id", equalTo(createdBlockerId.intValue()));
    }

    @Test
    @Order(3)
    void requirement3_shouldUpdateStatusAndDeleteNote() {
        // GIVEN an existing BLOCKER note
        // WHEN PUT /api/v1/notes/{id} updates status to RESOLVED
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "status": "RESOLVED",
                  "summary": "Firewall rule approved and verified in client VPC"
                }
                """)
        .when()
            .put("/api/v1/notes/" + createdBlockerId)
        .then()
            // THEN 200 OK is returned with updated status
            .statusCode(200)
            .body("status", equalTo("RESOLVED"));

        // WHEN DELETE /api/v1/notes/{id} is called
        given()
        .when()
            .delete("/api/v1/notes/" + createdBlockerId)
        .then()
            // THEN 204 No Content is returned
            .statusCode(204);

        // AND subsequent DELETE returns 404 Not Found
        given()
        .when()
            .delete("/api/v1/notes/" + createdBlockerId)
        .then()
            .statusCode(404);
    }
}
