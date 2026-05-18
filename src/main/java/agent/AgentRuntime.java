package agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import llm.OllamaClientWrapper;
import memory.ConversationMemory;
import model.Message;
import model.ToolCallRequest;
import io.github.ollama4j.tools.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentRuntime {
    private final AgentConfig config;
    private final Map<String, Tools.Tool> toolsMap;
    private final OllamaClientWrapper ollamaClient;
    private final ObjectMapper mapper;

    public AgentRuntime(AgentConfig config, List<Tools.Tool> toolsList) {
        this.config = config;
        this.toolsMap = new HashMap<>();
        for (Tools.Tool t : toolsList) {
            this.toolsMap.put(t.getToolSpec().getName(), t);
        }
        this.ollamaClient = new OllamaClientWrapper(config.getHost(), config.getModelName());
        this.mapper = new ObjectMapper();
    }

    public void run(ConversationMemory memory) throws Exception {
        injectSystemPrompt(memory);
        
        int iteration = 0;
        
        while (iteration < config.getMaxIterations()) {
            System.out.println("--- Agent Iteration " + (iteration + 1) + " ---");
            
            // 1. Send memory to LLM
            String response = ollamaClient.sendChatRequest(memory);
            System.out.println("LLM Response: " + response);
            
            // 2. Parse response for tool calls
            List<ToolCallRequest> toolCalls = parseToolCalls(response);
            
            if (toolCalls.isEmpty()) {
                // If no tool call, this is the final answer
                memory.addAssistantMessage(response);
                System.out.println("Agent finished reasoning.");
                break;
            } else {
                // Add assistant message with tool calls for context
                memory.addMessage(Message.assistantWithToolCalls(response, toolCalls));
                
                // 3. Execute tools
                for (ToolCallRequest call : toolCalls) {
                    System.out.println("Executing tool: " + call.getTool() + " with args: " + call.getArguments());
                    String resultStr;
                    try {
                        if (toolsMap.containsKey(call.getTool())) {
                            Tools.Tool tool = toolsMap.get(call.getTool());
                            Object result = tool.getToolFunction().apply(call.getArguments());
                            resultStr = mapper.writeValueAsString(result);
                        } else {
                            resultStr = "Error: Tool not found - " + call.getTool();
                        }
                    } catch (Exception e) {
                        resultStr = "Error executing tool: " + e.getMessage();
                    }
                    
                    System.out.println("Tool Result: " + resultStr);
                    // 4. Feed result back to memory
                    memory.addMessage(Message.toolResult(call.getTool(), resultStr));
                }
            }
            
            iteration++;
        }
        
        if (iteration >= config.getMaxIterations()) {
            System.out.println("Warning: Agent reached maximum iterations (" + config.getMaxIterations() + ").");
        }
    }

    private void injectSystemPrompt(ConversationMemory memory) {
        // If there's already a system message, we could append to it, but for simplicity we prepend one.
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an intelligent agent capable of reasoning and using tools. ");
        prompt.append("You must decide whether to answer the user's request directly or use tools to gather information.\n\n");
        
        prompt.append("AVAILABLE TOOLS:\n");
        for (Tools.Tool tool : toolsMap.values()) {
            prompt.append("- ").append(tool.getToolSpec().getName()).append(": ").append(tool.getToolSpec().getDescription()).append("\n");
            try {
                prompt.append("  Schema: ").append(mapper.writeValueAsString(tool.getToolSpec().getParameters())).append("\n\n");
            } catch (JsonProcessingException e) {
                prompt.append("  Schema: {}\n\n");
            }
        }
        
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("If you need to use a tool, you MUST reply ONLY with a valid JSON array of tool call objects.\n");
        prompt.append("Do NOT wrap the JSON in markdown code blocks like ```json ... ```. Just return the raw JSON.\n");
        prompt.append("Example tool call format:\n");
        prompt.append("[\n  {\n    \"tool\": \"tool_name\",\n    \"arguments\": {\"arg1\": \"value1\"}\n  }\n]\n\n");
        prompt.append("If you have enough information to answer the user, provide your final response as plain text (NO JSON).");

        // Insert at the beginning
        memory.getMessages().add(0, Message.system(prompt.toString()));
    }

    private List<ToolCallRequest> parseToolCalls(String response) {
        List<ToolCallRequest> calls = new ArrayList<>();
        
        // Clean up potential markdown formatting
        String cleanResponse = response.trim();
        if (cleanResponse.startsWith("```json")) {
            cleanResponse = cleanResponse.substring(7);
        } else if (cleanResponse.startsWith("```")) {
            cleanResponse = cleanResponse.substring(3);
        }
        if (cleanResponse.endsWith("```")) {
            cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
        }
        cleanResponse = cleanResponse.trim();
        
        if (cleanResponse.startsWith("[") && cleanResponse.endsWith("]")) {
            try {
                JsonNode arrayNode = mapper.readTree(cleanResponse);
                if (arrayNode.isArray()) {
                    for (JsonNode node : arrayNode) {
                        if (node.has("tool") && node.has("arguments")) {
                            ToolCallRequest call = mapper.treeToValue(node, ToolCallRequest.class);
                            calls.add(call);
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                // Not valid JSON array of tools, likely normal text response or malformed JSON
                System.out.println("Could not parse response as tool calls (might be plain text): " + e.getMessage());
            }
        }
        return calls;
    }
}
