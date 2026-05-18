package io.github.ollama4j.api;

import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.response.Model;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OllamaResource {

    @Inject
    OllamaService ollamaService;

    @GET
    @Path("/models")
    public List<String> getModels() throws Exception {
        System.out.println("getModels called");
        return ollamaService.listModels().stream()
                .map(Model::getName)
                .collect(Collectors.toList());
    }

    @DELETE
    @Path("/models/{modelName}")
    public Response deleteModel(@PathParam("modelName") String modelName) {
        try {
            ollamaService.deleteModel(modelName);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/settings/host")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHost() {
        return ollamaService.getHost();
    }

    @POST
    @Path("/settings/host")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setHost(String host) {
        try {
            ollamaService.setHost(host);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/chat/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> chatStream(ChatRequest req) {
        return Multi.createFrom().emitter(emitter -> {
            new Thread(() -> {
                try {
                    OllamaChatRequest requestModel = OllamaChatRequestBuilder.builder()
                            .withModel(req.model)
                            .withMessage(OllamaChatMessageRole.USER, req.message)
                            .build();

                    ollamaService.getClient().chat(requestModel, message -> {
                        String response = message.getMessage().getResponse(); 
                        if (response != null) {
                            emitter.emit(response);
                        }
                    });
                    emitter.complete();
                } catch (Exception e) {
                    emitter.fail(e);
                }
            }).start();
        });
    }
}
