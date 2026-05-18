package llm;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import memory.ConversationMemory;
import model.Message;

import java.util.ArrayList;
import java.util.List;

public class OllamaClientWrapper {
    private final Ollama ollama;
    private final String modelName;

    public OllamaClientWrapper(String host, String modelName) {
        this.ollama = new Ollama(host);
        this.ollama.setRequestTimeoutSeconds(120);
        this.modelName = modelName;
    }

    public String sendChatRequest(ConversationMemory memory) throws Exception {
        List<OllamaChatMessage> messages = new ArrayList<>();
        
        for (Message msg : memory.getMessages()) {
            OllamaChatMessageRole role;
            String content = msg.getContent() != null ? msg.getContent() : "";
            switch (msg.getRole()) {
                case SYSTEM:
                    role = OllamaChatMessageRole.SYSTEM;
                    break;
                case ASSISTANT:
                    role = OllamaChatMessageRole.ASSISTANT;
                    break;
                case TOOL:
                    role = OllamaChatMessageRole.USER;
                    content = "Tool '" + msg.getToolName() + "' result: " + msg.getToolResult();
                    break;
                case USER:
                default:
                    role = OllamaChatMessageRole.USER;
                    break;
            }
            messages.add(new OllamaChatMessage(role, content));
        }
        
        OllamaChatRequest requestModel = OllamaChatRequest.builder()
                .withModel(modelName)
                .withMessages(messages)
                .build();
                
        OllamaChatResult chatResult = ollama.chat(requestModel, chunk -> {});
        
        return chatResult.getResponseModel().getMessage().getResponse();
    }
}
