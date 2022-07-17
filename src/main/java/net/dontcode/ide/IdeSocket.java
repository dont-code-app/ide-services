package net.dontcode.ide;

import io.smallrye.mutiny.Uni;
import net.dontcode.core.Message;
import net.dontcode.ide.preview.PreviewServiceClient;
import net.dontcode.common.session.SessionActionType;
import net.dontcode.common.session.SessionService;
import net.dontcode.common.websocket.MessageEncoderDecoder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ide", encoders = MessageEncoderDecoder.class, decoders = MessageEncoderDecoder.class)
@ApplicationScoped
public class IdeSocket {
    private static Logger log = LoggerFactory.getLogger(IdeSocket.class);

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Inject
    @RestClient
    PreviewServiceClient previewServiceClient;

    @Inject
    SessionService sessionService;

    @OnOpen
    public void onOpen(Session session) {
        log.info("Session opened");
        sessions.put(session.getId(), session);
        var clientType = session.getRequestParameterMap().getOrDefault("clientType", Collections.singletonList("none")).get(0);
        sessionService.createNewSession(session.getId(), clientType)
                .flatMap(createdSession -> {
                    log.debug("Session {} created.", session.getId());
                    return Uni.createFrom().future(session.getAsyncRemote().sendObject(new Message(Message.MessageType.INIT, session.getId())));
                })
                .onFailure().call(throwable -> {
                    log.error("Error {} while saving session", throwable.getMessage());
                    return  Uni.createFrom().future(session.getAsyncRemote().sendObject(new Message(Message.MessageType.ERROR, throwable.getMessage())));
                }).subscribe().with(unused -> {});
    }

    @OnClose
    public void onClose(Session session) {
        log.info("Session closed");

        sessionService.updateSessionStatus(session.getId(), SessionActionType.CLOSE).subscribe().with(session1 -> {});
        String key[] = new String[1];
        sessions.entrySet().forEach(stringSessionEntry -> {
            if (stringSessionEntry.getValue()==session)
                key[0]=stringSessionEntry.getKey();
        });
        sessions.remove(key[0]);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("Error {}", throwable.getMessage());
        sessionService.updateSessionStatus(session.getId(), SessionActionType.ERROR).subscribe().with(session1 -> {});
    }

    @OnMessage
    public void onMessage(Message message, Session session) {
        log.debug("Message Received");
        log.trace("{}", message);

        sessionService.updateSession(session.getId(), message.getChange())
                .flatMap(updatedSession -> {
                    log.debug("Session {} updated.", session.getId());
                    return Uni.createFrom().future(session.getAsyncRemote().sendText("{ \"result\":\"Success\", \"SessionId\":\""+session.getId()+"\"}"));
                })
                .onFailure().call(throwable -> {
                    log.error("Error {} while updating session", throwable.getMessage());
                    return Uni.createFrom().future(session.getAsyncRemote().sendText("{ \"result\":\"Error\", \"ErrorMessage\":\"" + throwable.getMessage() + "\"}"));
            }).subscribe().with(unused -> {});

        if( message.getType()!= Message.MessageType.INIT) {
            if( message.getSessionId()==null) message.setSessionId(session.getId());
            previewServiceClient.receiveUpdate(message).subscribe().with(unused -> {
            }, throwable -> {
                log.error("Error calling previewService {}", throwable.getMessage());
            });
        }

    }

    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    log.error ("Unable to send message: {}", result.getException());
                }
            });
        });
    }
}
