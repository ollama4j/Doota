package io.github.ollama4j.api;

import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.chat.OllamaChatToolCalls;
import io.github.ollama4j.models.request.OllamaChatEndpointCaller;
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
    @Produces("application/x-ndjson")
    public Multi<java.util.Map<String, Object>> chatStream(ChatRequest req) {
        return Multi.createFrom().emitter(emitter -> {
            new Thread(() -> {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

                    List<OllamaChatMessage> history = new java.util.ArrayList<>();
                    if (req.messages != null) {
                        for (ChatRequest.Message m : req.messages) {
                            OllamaChatMessageRole role;
                            try {
                                role = OllamaChatMessageRole.getRole(m.role.toLowerCase());
                            } catch(Exception e) {
                                role = OllamaChatMessageRole.USER;
                            }
                            OllamaChatMessage msg = new OllamaChatMessage(role, m.content != null ? m.content : "");
                            if (m.tool_calls != null && !m.tool_calls.isEmpty()) {
                                List<OllamaChatToolCalls> tcs = new java.util.ArrayList<>();
                                for (ChatRequest.ToolCall tc : m.tool_calls) {
                                    OllamaChatToolCalls ollamaTc = new OllamaChatToolCalls();
                                    ollamaTc.setId(tc.id);
                                    if (tc.function != null) {
                                        io.github.ollama4j.tools.OllamaToolCallsFunction func = new io.github.ollama4j.tools.OllamaToolCallsFunction();
                                        func.setName(tc.function.name);
                                        func.setArguments(tc.function.arguments);
                                        ollamaTc.setFunction(func);
                                    }
                                    tcs.add(ollamaTc);
                                }
                                msg.setToolCalls(tcs);
                            }
                            history.add(msg);
                        }
                    }

                    OllamaChatRequest requestModel = OllamaChatRequest.builder()
                            .withModel(req.model)
                            .withMessages(history)
                            .withStreaming()
                            .build();

                    List<Tools.Tool> enabledTools = ollamaService.getEnabledTools();
                    if (!enabledTools.isEmpty()) {
                        requestModel.setTools(enabledTools);
                        requestModel.setUseTools(true);
                    }

                    // Track timing for tokens-per-second calculation
                    final long startNs = System.nanoTime();
                    final int[] tokenCount = {0};

                    String cleanHost = ollamaService.getHost();
                    if (cleanHost != null && cleanHost.endsWith("/")) {
                        cleanHost = cleanHost.substring(0, cleanHost.length() - 1);
                    }
                    OllamaChatEndpointCaller requestCaller = new OllamaChatEndpointCaller(
                            cleanHost,
                            null,
                            60
                    );

                    OllamaChatResult result = requestCaller.call(requestModel, chunk -> {
                        String response = chunk.getMessage().getResponse();
                        if (response != null && !response.isEmpty()) {
                            tokenCount[0]++;
                            try {
                                java.util.Map<String, Object> event = new java.util.HashMap<>();
                                event.put("type", "text");
                                event.put("content", response);
                                long elapsedNs = System.nanoTime() - startNs;
                                if (tokenCount[0] > 0 && elapsedNs > 0) {
                                    double liveTps = tokenCount[0] * 1_000_000_000.0 / elapsedNs;
                                    event.put("tps", Math.round(liveTps * 10.0) / 10.0);
                                }
                                emitter.emit(event);
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
                            tcMap.put("id", tc.getId());
                            tcMap.put("type", "function");
                            tcs.add(tcMap);
                        }
                        event.put("toolCalls", tcs);
                        emitter.emit(event);
                    }

                    // Emit done event with tokens-per-second
                    long elapsedNs = System.nanoTime() - startNs;
                    if (tokenCount[0] > 0 && elapsedNs > 0) {
                        double tps = tokenCount[0] * 1_000_000_000.0 / elapsedNs;
                        java.util.Map<String, Object> doneEvent = new java.util.HashMap<>();
                        doneEvent.put("type", "done");
                        doneEvent.put("tps", Math.round(tps * 10.0) / 10.0);
                        try {
                            emitter.emit(doneEvent);
                        } catch (Exception ex) {
                            // ignore
                        }
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

    // ─── Chat persistence endpoints ───────────────────────────────────────────

    @GET
    @Path("/chats")
    public List<ConversationDTO> listChats() {
        return ollamaService.listConversations();
    }

    @GET
    @Path("/chats/{id}")
    public Response getChat(@PathParam("id") String id) {
        ConversationDTO dto = ollamaService.getConversation(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    @Path("/chats/{id}")
    public Response saveChat(@PathParam("id") String id, ConversationDTO dto) {
        try {
            if (dto == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(java.util.Map.of("error", "Body required")).build();
            }
            // Ensure the id in the path matches the body
            dto.id = id;
            ollamaService.saveConversation(dto);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(java.util.Map.of("error", e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/chats/{id}")
    public Response deleteChat(@PathParam("id") String id) {
        boolean deleted = ollamaService.deleteConversation(id);
        if (deleted) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static class TitleRequestDTO {
        public String model;
        public String firstMessage;
    }

    @POST
    @Path("/chats/{id}/generate-title")
    public Response generateTitle(@PathParam("id") String id, TitleRequestDTO req) {
        try {
            if (req == null || req.model == null || req.firstMessage == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(java.util.Map.of("error", "model and firstMessage required")).build();
            }
            String prompt = "You are a helpful assistant. Generate a very concise and clear title (15 words maximum, without quotes, without conversational filler or colons) summarizing this user prompt: \"" + req.firstMessage + "\".";
            
            io.github.ollama4j.models.generate.OllamaGenerateRequest request = io.github.ollama4j.models.generate.OllamaGenerateRequest.builder()
                .withModel(req.model)
                .withPrompt(prompt)
                .build();
            
            io.github.ollama4j.models.response.OllamaResult result = ollamaService.getClient().generate(request, null);
            String title = result.getResponse().trim();
            // clean up surrounding quotes if any
            if (title.startsWith("\"") && title.endsWith("\"")) {
                title = title.substring(1, title.length() - 1);
            }
            if (title.startsWith("'") && title.endsWith("'")) {
                title = title.substring(1, title.length() - 1);
            }
            title = title.trim();

            // Programmatic fallback: if the model ignored instructions and outputted strictly all-caps,
            // convert it to a beautiful Title Case.
            if (title.length() > 0 && title.equals(title.toUpperCase())) {
                StringBuilder sb = new StringBuilder();
                boolean nextTitleCase = true;
                for (char c : title.toLowerCase().toCharArray()) {
                    if (Character.isWhitespace(c) || c == '-' || c == '_') {
                        nextTitleCase = true;
                    } else if (nextTitleCase) {
                        c = Character.toUpperCase(c);
                        nextTitleCase = false;
                    }
                    sb.append(c);
                }
                title = sb.toString();
            }
            
            // Also, update the title on the server's persisted JSON file!
            ConversationDTO dto = ollamaService.getConversation(id);
            if (dto != null) {
                dto.title = title;
                ollamaService.saveConversation(dto);
            }
            
            return Response.ok(java.util.Map.of("title", title)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(java.util.Map.of("error", e.getMessage())).build();
        }
    }
}

