package org.dontcode.ide;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.dontcode.ide.preview.PreviewServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(IdeTestResource.class)
public class IdeTestResourceTest {

    @InjectMock
    @RestClient
    PreviewServiceClient previewService;

    @Test
    public void testTestEndpoint() {
        String testString = "{\"name\":\"pizza\"}";

        //Mockito.when (previewService.receiveUpdate(testString)).thenAnswer ();
        given()
                .contentType(ContentType.JSON)
                .body(testString)
          .when().post("/")
          .then()
             .statusCode(HttpStatus.SC_OK);
    }

}