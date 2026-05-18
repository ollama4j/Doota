package io.github.ollama4j.api;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.response.Model;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class OllamaService {
    private Ollama ollama;
    private String host = "http://localhost:11434";

    public OllamaService() {
        initOllama();
    }

    private void initOllama() {
        this.ollama = new Ollama(this.host);
        this.ollama.setRequestTimeoutSeconds(60);
    }

    public Ollama getClient() {
        return ollama;
    }

    public List<Model> listModels() throws Exception {
        return ollama.listModels();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
        initOllama();
    }

    public void deleteModel(String modelName) throws Exception {
        ollama.deleteModel(modelName, true);
    }
}
