package io.github.ollama4j.api;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.tools.ToolFunction;
import io.github.ollama4j.tools.Tools;
import jakarta.enterprise.context.ApplicationScoped;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class OllamaService {
    private Ollama ollama;
    private String host = "http://localhost:11434";
    private boolean enableTools = true;

    private final List<ToolInfo> toolsList = new ArrayList<>();
    private final Map<String, Tools.Tool> toolsMap = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public static class Settings {
        public String ollamaUrl = "http://localhost:11434";
        public boolean enableTools = true;
    }

    public OllamaService() {
        loadSettings();
        initOllama();
        initTools();
    }

    // ─── Filesystem layout ────────────────────────────────────────────────────

    /** Root directory: ~/doota */
    private File getDootaHome() {
        String home = System.getProperty("user.home");
        File dir = new File(home, "doota");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** Chat storage directory: ~/doota/chats */
    private File getChatsDir() {
        File dir = new File(getDootaHome(), "chats");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getSettingsFile() {
        return new File(getDootaHome(), "settings.json");
    }

    private void loadSettings() {
        File file = getSettingsFile();
        if (file.exists()) {
            try {
                Settings settings = mapper.readValue(file, Settings.class);
                if (settings.ollamaUrl != null && !settings.ollamaUrl.trim().isEmpty()) {
                    this.host = settings.ollamaUrl;
                }
                this.enableTools = settings.enableTools;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            saveSettings();
        }
    }

    private void saveSettings() {
        try {
            Settings settings = new Settings();
            settings.ollamaUrl = this.host;
            settings.enableTools = this.enableTools;
            mapper.writerWithDefaultPrettyPrinter().writeValue(getSettingsFile(), settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initOllama() {
        this.ollama = new Ollama(this.host);
        this.ollama.setRequestTimeoutSeconds(60);
    }

    // ─── Tools YAML config ────────────────────────────────────────────────────

    private File getToolsConfigFile() {
        return new File(getDootaHome(), "tools.yaml");
    }

    /**
     * Copies the bundled default tools.yaml from resources to ~/doota/tools.yaml
     * so the user can customise it without touching the classpath.
     */
    private void ensureDefaultToolsConfig() {
        File dest = getToolsConfigFile();
        if (dest.exists()) return;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("tools.yaml")) {
            if (in == null) {
                System.err.println("[initTools] bundled tools.yaml not found in classpath");
                return;
            }
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[initTools] failed to copy default tools.yaml: " + e.getMessage());
        }
    }

    /**
     * Loads tools from ~/doota/tools.yaml (or falls back to the classpath default).
     * Each entry must specify a fully-qualified {@code functionClass} that implements
     * {@link ToolFunction} and has a public no-arg constructor.
     */
    private void initTools() {
        ensureDefaultToolsConfig();

        InputStream yamlStream;
        File userConfig = getToolsConfigFile();
        try {
            yamlStream = userConfig.exists()
                    ? new FileInputStream(userConfig)
                    : getClass().getClassLoader().getResourceAsStream("tools.yaml");
        } catch (FileNotFoundException e) {
            System.err.println("[initTools] could not open tools.yaml: " + e.getMessage());
            return;
        }

        if (yamlStream == null) {
            System.err.println("[initTools] tools.yaml not found");
            return;
        }

        ToolsConfig config;
        try (InputStream stream = yamlStream) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(ToolsConfig.class, loaderOptions));
            config = yaml.load(stream);
        } catch (IOException e) {
            System.err.println("[initTools] failed to parse tools.yaml: " + e.getMessage());
            return;
        }

        if (config == null || config.getTools() == null) {
            System.err.println("[initTools] tools.yaml is empty or malformed");
            return;
        }

        for (ToolsConfig.ToolEntry entry : config.getTools()) {
            try {
                // Build parameter map
                Map<String, Tools.Property> props = new LinkedHashMap<>();
                if (entry.getParameters() != null) {
                    for (ToolsConfig.ParameterEntry p : entry.getParameters()) {
                        props.put(p.getName(), Tools.Property.builder()
                                .type(p.getType())
                                .description(p.getDescription())
                                .required(p.isRequired())
                                .build());
                    }
                }

                // Build ToolSpec
                Tools.ToolSpec spec = new Tools.ToolSpec();
                spec.setName(entry.getName());
                spec.setDescription(entry.getDescription());
                spec.setParameters(Tools.Parameters.of(props));

                // Reflectively instantiate the ToolFunction
                Class<?> clazz = Class.forName(entry.getFunctionClass());
                ToolFunction fn = (ToolFunction) clazz.getDeclaredConstructor().newInstance();

                Tools.Tool tool = Tools.Tool.builder()
                        .toolSpec(spec)
                        .toolFunction(fn)
                        .build();

                toolsMap.put(entry.getName(), tool);
                toolsList.add(new ToolInfo(
                        entry.getName(),
                        entry.getDisplayName() != null ? entry.getDisplayName() : entry.getName(),
                        entry.getDescription() != null ? entry.getDescription() : "",
                        entry.isEnabled()));

            } catch (Exception e) {
                System.err.println("[initTools] skipping tool '" + entry.getName() + "': " + e.getMessage());
            }
        }
    }


    public List<ToolInfo> getToolsList() {
        return toolsList;
    }

    public void setToolEnabled(String name, boolean enabled) {
        for (ToolInfo info : toolsList) {
            if (info.getName().equals(name)) {
                info.setEnabled(enabled);
                break;
            }
        }
    }

    public List<Tools.Tool> getEnabledTools() {
        if (!enableTools) return new ArrayList<>();
        List<Tools.Tool> enabledTools = new ArrayList<>();
        for (ToolInfo info : toolsList) {
            if (info.isEnabled()) {
                Tools.Tool tool = toolsMap.get(info.getName());
                if (tool != null) {
                    enabledTools.add(tool);
                }
            }
        }
        return enabledTools;
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
        saveSettings();
    }

    public boolean isEnableTools() {
        return enableTools;
    }

    public void setEnableTools(boolean enableTools) {
        this.enableTools = enableTools;
        saveSettings();
    }

    public void deleteModel(String modelName) throws Exception {
        ollama.deleteModel(modelName, true);
    }

    // ─── Chat persistence ─────────────────────────────────────────────────────

    /** Returns all conversations sorted newest-first (by file last-modified). */
    public List<ConversationDTO> listConversations() {
        File dir = getChatsDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        List<ConversationDTO> result = new ArrayList<>();
        if (files == null) return result;
        // sort newest first
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : files) {
            if (f.length() == 0) continue; // skip empty/incomplete files
            try {
                ConversationDTO dto = mapper.readValue(f, ConversationDTO.class);
                result.add(dto);
            } catch (IOException e) {
                System.err.println("Skipping corrupt chat file: " + f.getName() + " — " + e.getMessage());
            }
        }
        return result;
    }

    /** Reads a single conversation by id, or null if not found. */
    public ConversationDTO getConversation(String id) {
        File f = new File(getChatsDir(), id + ".json");
        if (!f.exists()) return null;
        try {
            return mapper.readValue(f, ConversationDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Creates or overwrites the conversation file for the given DTO. */
    public void saveConversation(ConversationDTO dto) throws IOException {
        File f = new File(getChatsDir(), dto.id + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(f, dto);
    }

    /** Deletes the conversation file. Returns true if the file existed. */
    public boolean deleteConversation(String id) {
        File f = new File(getChatsDir(), id + ".json");
        return f.exists() && f.delete();
    }
}

