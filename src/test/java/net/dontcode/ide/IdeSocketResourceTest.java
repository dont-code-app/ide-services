package net.dontcode.ide;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import net.dontcode.common.test.mongo.AbstractMongoTest;
import net.dontcode.common.test.mongo.MongoTestProfile;
import net.dontcode.core.Message;
import net.dontcode.ide.preview.PreviewServiceClient;
import net.dontcode.common.session.SessionService;
import net.dontcode.common.websocket.MessageEncoderDecoder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

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
            net.dontcode.common.session.Session savedSession=null;
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
            Mockito.verify(previewService, Mockito.times(0)).receiveUpdate(Mockito.any(Message.class));
        }
    }

    @ClientEndpoint(encoders = MessageEncoderDecoder.class, decoders = MessageEncoderDecoder.class)
    public static class ClientTestSession {

        public static String sessionId=null;

        @OnOpen
        public void open(Session session) {
            //MESSAGES.add("CONNECT");
            // Send a message to indicate that we are ready,
            // as the message handler may not be registered immediately after this callback.
            //session.getAsyncRemote().sendObject(new Message(Message.MessageType.INIT, ));
        }

        @OnMessage
        void message(Message msg) {
            Assertions.assertEquals(msg.getType(), Message.MessageType.INIT);
            Assertions.assertNotNull(msg.getSessionId());

            sessionId = msg.getSessionId();
        }

        @OnError
        void error (Throwable error) {
            System.err.println("Error "+ error.getMessage());
        }

    }
}
