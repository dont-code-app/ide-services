package net.dontcode.ide;

import io.smallrye.mutiny.Uni;
import net.dontcode.core.Message;
import net.dontcode.ide.preview.PreviewServiceClient;
import net.dontcode.common.session.SessionService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test")
public class IdeTestResource {
    private static Logger log = LoggerFactory.getLogger(IdeTestResource.class);

    @Inject
    @RestClient
    PreviewServiceClient previewServiceClient;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> testAsIde(Message update) {
        log.debug("Receiving from test");
        log.trace("{}", update);

        if( update.getType()!= Message.MessageType.INIT) {
            return previewServiceClient.receiveUpdate(update).map(unused -> {
                return Response.ok().build();
            }).onFailure().recoverWithItem(throwable -> {
                log.error("Error calling previewService {}", throwable.getMessage());
                return Response.serverError().entity("Error calling Preview Service:"+throwable.getMessage()).build();
            });
        }
        return Uni.createFrom().item(Response.ok().build());
    }
}
