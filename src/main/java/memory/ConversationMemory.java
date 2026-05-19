package memory;

import model.Message;
import java.util.ArrayList;
import java.util.List;

public class ConversationMemory {
    private final List<Message> messages;

    public ConversationMemory() {
        this.messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public void addSystemMessage(String content) {
        messages.add(Message.system(content));
    }

    public void addUserMessage(String content) {
        messages.add(Message.user(content));
    }

    public void addAssistantMessage(String content) {
        messages.add(Message.assistant(content));
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
    }
}
