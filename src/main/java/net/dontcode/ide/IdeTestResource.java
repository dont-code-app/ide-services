package net.dontcode.ide;

import io.smallrye.mutiny.Uni;
import net.dontcode.core.Message;
import net.dontcode.ide.preview.PreviewServiceClient;
import net.dontcode.ide.session.SessionService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test")
public class IdeTestResource {
    private static Logger log = LoggerFactory.getLogger(IdeTestResource.class);

    @Inject
    @RestClient
    PreviewServiceClient previewServiceClient;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testAsIde(Message update) {
        log.debug("Receiving from test");
        log.trace("{}", update);

        if( update.getType()!= Message.MessageType.INIT) {
            previewServiceClient.receiveUpdate(update);
        }
        return Response.ok().build();
    }
}