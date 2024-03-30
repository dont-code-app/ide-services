package net.dontcode.ide.preview;

import io.smallrye.mutiny.Uni;
import net.dontcode.core.Message;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/messages")
@ApplicationScoped
@RegisterRestClient(configKey = "preview-service")
public interface PreviewServiceClient {
        @POST
        @Consumes("application/json")
        Uni<Response> receiveUpdate (Message update);
}
