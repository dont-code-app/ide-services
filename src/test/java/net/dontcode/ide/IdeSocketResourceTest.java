package net.dontcode.ide;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import net.dontcode.common.session.SessionService;
import net.dontcode.common.test.mongo.AbstractMongoTest;
import net.dontcode.common.test.mongo.MongoTestProfile;
import net.dontcode.common.websocket.MessageEncoderDecoder;
import net.dontcode.core.Message;
import net.dontcode.ide.preview.PreviewServiceClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
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

    /**
     * Test that upon opening a session, we receive an INIT message with the session Id
     * @throws DeploymentException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testSession() throws DeploymentException, IOException, InterruptedException {
        //Mockito.when(previewService.receiveUpdate(Mockito.anyString())).thenThrow(new RuntimeException("Errorrrrererre"));//Return(Uni.createFrom().voidItem());
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(ClientTestSession.class, uri)) {
            String[] saveSession = {""};
            net.dontcode.common.session.Session sessionFound = ClientTestSession.session.flatMap(sessionId -> {
                saveSession[0]=sessionId;
                if( sessionId!=null) {
                    return sessionService.findSessionCreationEvent(sessionId);
                }
                else {
                    throw new Error ("Null Session received");
                }
            }).await().atMost(Duration.ofMinutes(1));
            session.close();
            Assertions.assertNotNull(saveSession[0], "Session INIT was not received.");
            Assertions.assertEquals(saveSession[0], sessionFound.id(), "Session has not been found");
            //System.out.println("Closing session");
            Mockito.verify(previewService, Mockito.times(0)).receiveUpdate(Mockito.any(Message.class));
        }
    }

    @ClientEndpoint(encoders = MessageEncoderDecoder.class, decoders = MessageEncoderDecoder.class)
    public static class ClientTestSession {

        // Creates a Uni that will send the sessionsId received
        public static Uni<String> session = Uni.createFrom().emitter(uniEmitter -> {
            ClientTestSession.emitter = uniEmitter;
        });
        protected static UniEmitter<? super String> emitter;

        @OnOpen
        public void open(Session session) {
            //System.out.println("Session open");
        }

        @OnMessage
        void message(Message msg) {
//            System.out.println("Message received");
            Assertions.assertEquals(msg.getType(), Message.MessageType.INIT);
            Assertions.assertNotNull(msg.getSessionId());

            emitter.complete( msg.getSessionId());
        }

        @OnClose
        void close () {
//            System.out.println ("Client Session closed");
        }

        @OnError
        void error (Throwable error) {
            System.err.println("Error "+ error.getMessage());
            Assertions.fail(error.getMessage());
        }

    }
}
