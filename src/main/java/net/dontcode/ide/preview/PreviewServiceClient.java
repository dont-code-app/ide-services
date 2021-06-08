package net.dontcode.ide.preview;

import io.smallrye.mutiny.Uni;
import net.dontcode.core.Message;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/messages")
@ApplicationScoped
@RegisterRestClient(configKey = "preview-service")
public interface PreviewServiceClient {
        @POST
        @Consumes("application/json")
        Uni<Response> receiveUpdate (Message update);
}
