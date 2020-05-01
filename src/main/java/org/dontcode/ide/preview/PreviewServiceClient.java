package org.dontcode.ide.preview;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/updates")
@RegisterRestClient(configKey = "preview-service")
public interface PreviewServiceClient {
        @POST
        @Consumes("application/json")
        void receiveUpdate (String update);
}
