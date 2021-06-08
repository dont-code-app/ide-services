package net.dontcode.session;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.MongoClientName;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import net.dontcode.core.Change;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class SessionService {
    private static Logger log = LoggerFactory.getLogger(SessionService.class);

    @Inject
    @MongoClientName("projects")
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "projects-database-name")
    String projectDbName;

    public Uni<Session> createNewSession (String id,String srcInfo) {
        Session session = new Session(id, Instant.now(), SessionActionType.CREATE, srcInfo, null);
        return getSession().insertOne(session).map(insertOneResult -> {
            return session;
        }).onFailure().invoke(throwable -> {
            log.error("Error InsertingMongo {}", throwable.getMessage());
        });
    }

    public Uni<Session> findSessionCreationEvent (String id) {
        log.debug("Querying for {}", id);
        return getSession().find(and(Filters.eq("id", id), Filters.eq( "type",SessionActionType.CREATE.name()))).toUni();
    }

    public Uni<Session> updateSession (String id, Change change) {
        Session session = new Session(id, Instant.now(), SessionActionType.UPDATE, null, change);
        return getSession().insertOne(session).map(insertOneResult -> {
            return session;
        }).onFailure().invoke(throwable -> {
            log.error("Error InsertingMongo {}", throwable.getMessage());
        });
    }

    public Uni<Session> updateSessionStatus (String id, SessionActionType newAction) {
        Session session = new Session(id, Instant.now(), newAction, null,null);
        return getSession().insertOne(session).map(insertOneResult -> {
            return session;
        }).onFailure().invoke(throwable -> {
            log.error("Error InsertingMongo {}", throwable.getMessage());
        });
    }

    public Multi<Session> listSessionsInOrder (String id) {

        return getSession().find(new FindOptions().filter(eq("id", id)).sort(Sorts.ascending("time"))).onFailure().invoke(throwable -> {
            log.error("Error Listing Session from Mongo {}", throwable.getMessage());
        });
    }

    protected ReactiveMongoCollection<Session> getSession() {
        return getDatabase().getCollection("sessions", Session.class);
    }

    protected ReactiveMongoDatabase getDatabase () {
        return mongoClient.getDatabase(projectDbName);
    }
}
