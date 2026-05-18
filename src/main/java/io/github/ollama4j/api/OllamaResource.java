package io.github.ollama4j.api;

import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.chat.OllamaChatToolCalls;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.tools.Tools;
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

    @Inject
    AgentService agentService;

    @GET
    @Path("/models")
    public List<Model> getModels() throws Exception {
        return ollamaService.listModels();
    }

    @GET
    @Path("/models/loaded")
    public List<String> getLoadedModels() {
        try {
            return ollamaService.getClient().ps().getModels().stream()
                    .map(m -> m.getName())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @GET
    @Path("/models/details/{modelName}")
    public Object getModelDetails(@PathParam("modelName") String modelName) {
        try {
            return ollamaService.getClient().getModelDetails(modelName);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/models/unload/{modelName}")
    public Response unloadModel(@PathParam("modelName") String modelName) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ollamaService.getHost() + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            String.format("{\"model\": \"%s\", \"keep_alive\": 0}", modelName)
                    ))
                    .build();
            client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
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

    @GET
    @Path("/settings/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pingHost() {
        try {
            boolean reachable = ollamaService.getClient().ping();
            return Response.ok(java.util.Map.of("reachable", reachable)).build();
        } catch (Exception e) {
            return Response.ok(java.util.Map.of("reachable", false)).build();
        }
    }

    @GET
    @Path("/settings/tools")
    public List<ToolInfo> getTools() {
        return ollamaService.getToolsList();
    }

    @POST
    @Path("/settings/tools/{name}/toggle")
    public List<ToolInfo> toggleTool(@PathParam("name") String name) {
        ollamaService.setToolEnabled(name, !ollamaService.getToolsList().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .map(ToolInfo::isEnabled)
                .orElse(false));
        return ollamaService.getToolsList();
    }

    @POST
    @Path("/chat/stream")
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> chatStream(ChatRequest req) {
        return Multi.createFrom().emitter(emitter -> {
            new Thread(() -> {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

                    List<OllamaChatMessage> history = new java.util.ArrayList<>();
                    if (req.messages != null) {
                        for (ChatRequest.Message m : req.messages) {
                            OllamaChatMessageRole role;
                            try {
                                role = OllamaChatMessageRole.getRole(m.role.toUpperCase());
                            } catch(Exception e) {
                                role = OllamaChatMessageRole.USER;
                            }
                            OllamaChatMessage msg = new OllamaChatMessage(role, m.content != null ? m.content : "");
                            history.add(msg);
                        }
                    }

                    OllamaChatRequest requestModel = OllamaChatRequest.builder()
                            .withModel(req.model)
                            .withMessages(history)
                            .build();

                    List<Tools.Tool> enabledTools = ollamaService.getEnabledTools();
                    if (!enabledTools.isEmpty()) {
                        requestModel.setTools(enabledTools);
                        requestModel.setUseTools(true);
                    }

                    OllamaChatResult result = ollamaService.getClient().chat(requestModel, chunk -> {
                        String response = chunk.getMessage().getResponse();
                        if (response != null && !response.isEmpty()) {
                            try {
                                java.util.Map<String, String> event = new java.util.HashMap<>();
                                event.put("type", "text");
                                event.put("content", response);
                                emitter.emit(mapper.writeValueAsString(event) + "\n");
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    });

                    if (result.getResponseModel() != null && result.getResponseModel().getMessage() != null && result.getResponseModel().getMessage().getToolCalls() != null && !result.getResponseModel().getMessage().getToolCalls().isEmpty()) {
                        java.util.Map<String, Object> event = new java.util.HashMap<>();
                        event.put("type", "tool_call");

                        List<java.util.Map<String, Object>> tcs = new java.util.ArrayList<>();
                        for (OllamaChatToolCalls tc : result.getResponseModel().getMessage().getToolCalls()) {
                            java.util.Map<String, Object> tcMap = new java.util.HashMap<>();
                            java.util.Map<String, Object> funcMap = new java.util.HashMap<>();
                            funcMap.put("name", tc.getFunction().getName());
                            funcMap.put("arguments", tc.getFunction().getArguments());
                            tcMap.put("function", funcMap);
                            tcs.add(tcMap);
                        }
                        event.put("toolCalls", tcs);
                        emitter.emit(mapper.writeValueAsString(event) + "\n");
                    }

                    emitter.complete();
                } catch (Exception e) {
                    emitter.fail(e);
                }
            }).start();
        });
    }

    @POST
    @Path("/tools/execute")
    public Response executeTool(ToolExecutionRequest req) {
        try {
            List<Tools.Tool> enabledTools = ollamaService.getEnabledTools();
            for (Tools.Tool tool : enabledTools) {
                if (tool.getToolSpec().getName().equals(req.toolName)) {
                    Object result = tool.getToolFunction().apply(req.arguments);
                    return Response.ok(result).build();
                }
            }
            return Response.status(Response.Status.NOT_FOUND).entity(java.util.Map.of("error", "Tool not found")).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(java.util.Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/agent/chat")
    public Response agentChat(AgentChatRequest req) {
        try {
            if (req == null || req.prompt == null || req.prompt.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(java.util.Map.of("error", "Prompt cannot be empty")).build();
            }
            memory.ConversationMemory memory = agentService.runAgent(req);
            return Response.ok(memory).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(java.util.Map.of("error", e.getMessage())).build();
        }
    }
}
