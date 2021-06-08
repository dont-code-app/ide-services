package net.dontcode.ide;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import net.dontcode.core.Change;
import net.dontcode.core.Message;
import org.apache.http.HttpStatus;
import net.dontcode.ide.preview.PreviewServiceClient;
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
        Message toSend = new Message(Message.MessageType.INIT, "TestSession1");

        given()
                .contentType(ContentType.JSON)
                .body(toSend)
          .when().post("/")
          .then()
             .statusCode(HttpStatus.SC_OK);
        // Verify that init messages are NOT broadcasted
        Mockito.verify(previewService, Mockito.times(0)).receiveUpdate(Mockito.any(Message.class));

        Change chg = new Change(Change.ChangeType.RESET, "/", null);
        toSend = new Message(Message.MessageType.CHANGE, chg);

        given()
                .contentType(ContentType.JSON)
                .body(toSend)
                .when().post("/")
                .then()
                .statusCode(HttpStatus.SC_OK);
        // Verify that init messages are NOT broadcasted
        Mockito.verify(previewService, Mockito.times(1)).receiveUpdate(Mockito.any(Message.class));
    }

}