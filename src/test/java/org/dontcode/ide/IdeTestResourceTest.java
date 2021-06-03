package org.dontcode.ide;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.dontcode.ide.preview.PreviewServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(IdeTestResource.class)
@TestProfile(MongoTestProfile.class)
public class IdeTestResourceTest extends AbstractMongoTest{

    @InjectMock
    @RestClient
    PreviewServiceClient previewService;

    @Test
    public void testTestEndpoint() {
        String testString = "{\"name\":\"pizza\"}";

        given()
                .contentType(ContentType.JSON)
                .body(testString)
          .when().post("/")
          .then()
             .statusCode(HttpStatus.SC_OK);
        Mockito.verify(previewService, Mockito.times(1)).receiveUpdate(Mockito.anyString());
    }

}