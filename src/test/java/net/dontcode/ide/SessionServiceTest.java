package net.dontcode.ide;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import net.dontcode.core.Change;
import net.dontcode.core.DontCodeModelPointer;
import net.dontcode.session.SessionService;
import net.dontcode.session.SessionActionType;
import org.bson.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
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

        Change chg = new Change(Change.ChangeType.ADD, "creation/name", "NewAppName");
        sessionService.updateSession(sessionId, chg).await().indefinitely();

        sessionService.updateSessionStatus(sessionId, SessionActionType.ERROR).await().indefinitely();

        var value = new JsonObject ("""
                {
                    "name":"EntityName",
                    "fields": [{
                        "name":"FieldA",
                        "type":"string"
                        }]
                }
                """).toBsonDocument();
        var pointer = new DontCodeModelPointer("creation/entities/a", "creation/entities","creation", "creation", null, "a");
        chg = new Change(Change.ChangeType.UPDATE, pointer.getPosition(), value, pointer);
        sessionService.updateSession(sessionId, chg).await().indefinitely();

        sessionService.updateSessionStatus(sessionId, SessionActionType.CLOSE).await().indefinitely();
        var listSessions = sessionService.listSessionsInOrder(sessionId).collect().asList().await().indefinitely();
        Assertions.assertEquals(listSessions.size(), 5);

        AtomicReference<Instant> oldTime=new AtomicReference<>(null);
        int[] counter = new int[1];
        counter[0]=0;
        listSessions.forEach(session -> {
            Assertions.assertTrue((oldTime.get() ==null) || (session.time().isAfter(oldTime.get())));
            oldTime.set(session.time());
            counter[0] = counter[0]+1;
            if( counter[0]==4) {    // We test thoroughly the update where we send a value as json
                Map jsonValue = (Map) session.change().getValue();
                Assertions.assertEquals(jsonValue.get("name"), "EntityName");
                Assertions.assertEquals(((Map)((List)jsonValue.get("fields")).get(0)).get("name"),"FieldA");
            }
        });

        Assertions.assertEquals(listSessions.get(listSessions.size()-1).type(), SessionActionType.CLOSE);

    }

}