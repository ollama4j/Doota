package io.github.ollama4j.api;

import agent.AgentConfig;
import agent.AgentRuntime;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import memory.ConversationMemory;

@ApplicationScoped
public class AgentService {

    @Inject
    OllamaService ollamaService;

    public ConversationMemory runAgent(AgentChatRequest req) throws Exception {
        // 1. Setup agent config
        AgentConfig config = new AgentConfig();
        config.setHost(ollamaService.getHost());
        // For demonstration, use a robust model that supports function calling well.
        config.setModelName("llama3.1:8b");
        config.setMaxIterations(5);

        // 2. Create Agent Runtime using native tools from OllamaService
        AgentRuntime agent = new AgentRuntime(config, ollamaService.getEnabledTools());

        // 3. Create Conversation
        ConversationMemory memory = new ConversationMemory();
        if (req.history != null) {
            for (AgentChatRequest.Message m : req.history) {
                if (m.role != null && m.content != null && !m.content.trim().isEmpty()) {
                    if (m.role.equalsIgnoreCase("user")) {
                        memory.addUserMessage(m.content);
                    } else if (m.role.equalsIgnoreCase("assistant")) {
                        memory.addAssistantMessage(m.content);
                    } else if (m.role.equalsIgnoreCase("system")) {
                        memory.getMessages().add(model.Message.system(m.content));
                    }
                }
            }
        }
        memory.addUserMessage(req.prompt);

        // 4. Run the agent
        agent.run(memory);

        return memory;
    }
}
