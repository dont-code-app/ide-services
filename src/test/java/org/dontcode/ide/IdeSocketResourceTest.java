package org.dontcode.ide;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.internal.path.json.JSONAssertion;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertionHelper;
import org.dontcode.ide.preview.PreviewServiceClient;
import org.dontcode.ide.session.SessionService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;
import javax.json.stream.JsonParserFactory;
import javax.websocket.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.Duration;

@QuarkusTest
@TestProfile(MongoTestProfile.class)
public class IdeSocketResourceTest extends AbstractMongoTest {

    @InjectMock
    @RestClient
    PreviewServiceClient previewService;

    @Inject
    SessionService sessionService;

    @TestHTTPResource("/ide")
    URI uri;

    @Test
    public void testSession() throws DeploymentException, IOException, InterruptedException {
        //Mockito.when(previewService.receiveUpdate(Mockito.anyString())).thenThrow(new RuntimeException("Errorrrrererre"));//Return(Uni.createFrom().voidItem());
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(ClientTestSession.class, uri)) {
            org.dontcode.ide.session.Session savedSession=null;
            // Wait the data to be saved in the database
            for (int i = 0; i < 10; i++) {
                Thread.sleep(50);
                if( ClientTestSession.sessionId!=null) {
                    savedSession = sessionService.findSessionCreationEvent(ClientTestSession.sessionId).await().indefinitely();
                    if (savedSession != null) {
                        Assertions.assertEquals(ClientTestSession.sessionId, savedSession.id());
                        break;
                    }
                }
            }
            Assertions.assertNotNull(savedSession, "Session was not saved to database");
            Mockito.verify(previewService, Mockito.times(1)).receiveUpdate(Mockito.anyString());
        }
    }

    @ClientEndpoint
    public static class ClientTestSession {

        public static String sessionId=null;

        @OnOpen
        public void open(Session session) {
            //MESSAGES.add("CONNECT");
            // Send a message to indicate that we are ready,
            // as the message handler may not be registered immediately after this callback.
            session.getAsyncRemote().sendText("_ready_");
        }

        @OnMessage
        void message(String msg) {
            //MESSAGES.add(msg);
            //System.out.println(msg);
            JsonObject response = Json.createReader(new StringReader(msg)).readObject();
            Assertions.assertEquals(response.getString("result"), "Success");

            sessionId = response.getString("SessionId");
        }

        @OnError
        void error (Throwable error) {
            System.err.println("Error "+ error.getMessage());
        }

    }
}