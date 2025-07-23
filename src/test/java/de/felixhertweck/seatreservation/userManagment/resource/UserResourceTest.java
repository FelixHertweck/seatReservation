package de.felixhertweck.seatreservation.userManagment.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class UserResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    public void testGetAllUsersAsAdmin() {
        given().when()
                .get("/api/users/admin")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @TestSecurity(user = "testuser", roles = "USER")
    public void testGetAllUsersAsAdminForbidden() {
        given().when().get("/api/users/admin").then().statusCode(403);
    }

    @Test
    public void testGetAllUsersAsAdminUnauthorized() {
        given().when().get("/api/users/admin").then().statusCode(401);
    }
}
