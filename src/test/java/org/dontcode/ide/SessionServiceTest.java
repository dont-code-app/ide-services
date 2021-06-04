package org.dontcode.ide;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.dontcode.ide.session.Session;
import org.dontcode.ide.session.SessionActionType;
import org.dontcode.ide.session.SessionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
@TestProfile(MongoTestProfile.class)
public class SessionServiceTest extends AbstractMongoTest {


    @Inject
    SessionService sessionService;

    @Test
    public void testSessionCreateFindUpdate() {
        var sessionId = UUID.randomUUID().toString();
        var createdSession = sessionService.createNewSession(sessionId, "Test Info for "+sessionId).await().indefinitely();
        Assertions.assertEquals(createdSession.id(), sessionId);
        Assertions.assertEquals(createdSession.type(), SessionActionType.CREATE);
        Assertions.assertNotNull(createdSession.time());

        var savedSession = sessionService.findSessionCreationEvent(sessionId).await().indefinitely();
        Assertions.assertNotNull(savedSession.time());

        BsonDocument doc = new BsonDocument().append("test", new BsonString("value1")).append("test2", new BsonString("value2"));
        sessionService.updateSession(sessionId, doc).await().indefinitely();

        sessionService.updateSessionStatus(sessionId, SessionActionType.ERROR).await().indefinitely();

        var doc2 = new BsonDocument().append("test3", new BsonString("value3"));
        sessionService.updateSession(sessionId, doc2).await().indefinitely();

        sessionService.updateSessionStatus(sessionId, SessionActionType.CLOSE).await().indefinitely();
        var listSessions = sessionService.listSessionsInOrder(sessionId).collect().asList().await().indefinitely();
        Assertions.assertEquals(listSessions.size(), 5);

        AtomicReference<Instant> oldTime=new AtomicReference<>(null);
        listSessions.forEach(session -> {
            Assertions.assertTrue((oldTime.get() ==null) || (session.time().isAfter(oldTime.get())));
            oldTime.set(session.time());
        });

        Assertions.assertEquals(listSessions.get(listSessions.size()-1).type(), SessionActionType.CLOSE);

    }

}