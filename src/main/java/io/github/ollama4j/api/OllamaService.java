package io.github.ollama4j.api;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.tools.Tools;
import io.github.ollama4j.api.tools.FindFilesByNameToolFunction;
import io.github.ollama4j.api.tools.FindFilesByExtensionToolFunction;
import io.github.ollama4j.api.tools.SystemInfoToolFunction;
import io.github.ollama4j.api.tools.PlatformTypeToolFunction;
import io.github.ollama4j.api.tools.GetHomeDirectoryToolFunction;
import io.github.ollama4j.api.tools.CalculatorTool;
import io.github.ollama4j.api.tools.DateTimeTool;
import io.github.ollama4j.api.tools.FileReadTool;
import io.github.ollama4j.api.tools.WeatherTool;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
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

    private File getSettingsFile() {
        String home = System.getProperty("user.home");
        File dir = new File(home, "ollama4j-ui");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "settings.json");
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

    private void initTools() {
        // 1. Find Files By Name
        Tools.ToolSpec byNameSpec = new Tools.ToolSpec();
        byNameSpec.setName("find_files_by_name");
        byNameSpec.setDescription(
            "Finds files on this computer whose filename contains the given name keyword (case-insensitive substring match). " +
            "Use this when the user wants to search for files by their name or a partial name keyword.");

        Map<String, Tools.Property> byNameProps = new LinkedHashMap<>();
        byNameProps.put("nameQuery", Tools.Property.builder()
                .type("string")
                .description("The keyword or substring to search for within filenames. Required.")
                .required(true)
                .build());
        byNameProps.put("searchDir", Tools.Property.builder()
                .type("string")
                .description("The directory to search in. Optional. Defaults to the user's home directory.")
                .required(false)
                .build());
        byNameProps.put("recursive", Tools.Property.builder()
                .type("boolean")
                .description("Whether to search directories recursively. Optional. Defaults to true.")
                .required(false)
                .build());
        byNameSpec.setParameters(Tools.Parameters.of(byNameProps));

        Tools.Tool byNameTool = Tools.Tool.builder()
                .toolSpec(byNameSpec)
                .toolFunction(new FindFilesByNameToolFunction())
                .build();

        // 2. Find Files By Extension
        Tools.ToolSpec byExtSpec = new Tools.ToolSpec();
        byExtSpec.setName("find_files_by_extension");
        byExtSpec.setDescription(
            "Finds files on this computer that match a specific file extension (e.g. 'pdf', 'txt', 'png', 'mp4'). " +
            "Use this when the user wants to search for files of a particular type or format.");

        Map<String, Tools.Property> byExtProps = new LinkedHashMap<>();
        byExtProps.put("extension", Tools.Property.builder()
                .type("string")
                .description("The file extension to search for, without the leading dot (e.g. 'pdf', 'txt', 'jpg'). Required.")
                .required(true)
                .build());
        byExtProps.put("searchDir", Tools.Property.builder()
                .type("string")
                .description("The directory to search in. Optional. Defaults to the user's home directory.")
                .required(false)
                .build());
        byExtProps.put("recursive", Tools.Property.builder()
                .type("boolean")
                .description("Whether to search directories recursively. Optional. Defaults to true.")
                .required(false)
                .build());
        byExtSpec.setParameters(Tools.Parameters.of(byExtProps));

        Tools.Tool byExtTool = Tools.Tool.builder()
                .toolSpec(byExtSpec)
                .toolFunction(new FindFilesByExtensionToolFunction())
                .build();

        // 3. System Info
        Tools.ToolSpec sysInfoSpec = new Tools.ToolSpec();
        sysInfoSpec.setName("system_info");
        sysInfoSpec.setDescription(
            "Retrieves system specifications of this computer: OS name/version, CPU cores, JVM memory, " +
            "disk drives with free/total space, username, and home directory.");
        sysInfoSpec.setParameters(Tools.Parameters.of(new HashMap<>()));

        Tools.Tool sysInfoTool = Tools.Tool.builder()
                .toolSpec(sysInfoSpec)
                .toolFunction(new SystemInfoToolFunction())
                .build();

        // 4. Platform Type
        Tools.ToolSpec platformTypeSpec = new Tools.ToolSpec();
        platformTypeSpec.setName("platform_type");
        platformTypeSpec.setDescription(
            "Identifies the platform type of this computer: linux, mac, or windows.");
        platformTypeSpec.setParameters(Tools.Parameters.of(new HashMap<>()));

        Tools.Tool platformTypeTool = Tools.Tool.builder()
                .toolSpec(platformTypeSpec)
                .toolFunction(new PlatformTypeToolFunction())
                .build();

        // 5. Get Home Directory
        Tools.ToolSpec getHomeDirSpec = new Tools.ToolSpec();
        getHomeDirSpec.setName("get_home_directory");
        getHomeDirSpec.setDescription(
            "Retrieves the absolute path of the user's home directory on this computer.");
        getHomeDirSpec.setParameters(Tools.Parameters.of(new HashMap<>()));

        Tools.Tool getHomeDirTool = Tools.Tool.builder()
                .toolSpec(getHomeDirSpec)
                .toolFunction(new GetHomeDirectoryToolFunction())
                .build();

        // 6. Calculator Tool
        Tools.ToolSpec calcSpec = new Tools.ToolSpec();
        calcSpec.setName("calculator");
        calcSpec.setDescription("Evaluates a mathematical expression. Use this to perform math calculations like addition, subtraction, multiplication, and division.");
        Map<String, Tools.Property> calcProps = new LinkedHashMap<>();
        calcProps.put("operation", Tools.Property.builder().type("string").description("The operation to perform (add, subtract, multiply, divide)").required(true).build());
        calcProps.put("a", Tools.Property.builder().type("number").description("First operand").required(true).build());
        calcProps.put("b", Tools.Property.builder().type("number").description("Second operand").required(true).build());
        calcSpec.setParameters(Tools.Parameters.of(calcProps));
        Tools.Tool calcTool = Tools.Tool.builder().toolSpec(calcSpec).toolFunction(new CalculatorTool()).build();
        
        // 7. DateTime Tool
        Tools.ToolSpec dateTimeSpec = new Tools.ToolSpec();
        dateTimeSpec.setName("get_current_datetime");
        dateTimeSpec.setDescription("Gets the current system date and time.");
        dateTimeSpec.setParameters(Tools.Parameters.of(new HashMap<>()));
        Tools.Tool dateTimeTool = Tools.Tool.builder().toolSpec(dateTimeSpec).toolFunction(new DateTimeTool()).build();

        // 8. Weather Tool
        Tools.ToolSpec weatherSpec = new Tools.ToolSpec();
        weatherSpec.setName("get_weather");
        weatherSpec.setDescription("Gets the current weather for a specified city or location.");
        Map<String, Tools.Property> weatherProps = new LinkedHashMap<>();
        weatherProps.put("location", Tools.Property.builder().type("string").description("The city and state, e.g. San Francisco, CA").required(true).build());
        weatherSpec.setParameters(Tools.Parameters.of(weatherProps));
        Tools.Tool weatherTool = Tools.Tool.builder().toolSpec(weatherSpec).toolFunction(new WeatherTool()).build();

        // 9. File Read Tool
        Tools.ToolSpec fileReadSpec = new Tools.ToolSpec();
        fileReadSpec.setName("read_file");
        fileReadSpec.setDescription("Reads the contents of a local file at the specified absolute path.");
        Map<String, Tools.Property> fileReadProps = new LinkedHashMap<>();
        fileReadProps.put("filePath", Tools.Property.builder().type("string").description("The absolute file path to read").required(true).build());
        fileReadSpec.setParameters(Tools.Parameters.of(fileReadProps));
        Tools.Tool fileReadTool = Tools.Tool.builder().toolSpec(fileReadSpec).toolFunction(new FileReadTool()).build();

        // Register all tools
        toolsMap.put("find_files_by_name", byNameTool);
        toolsList.add(new ToolInfo("find_files_by_name", "Find Files by Name",
                "Search for files on your computer whose filename matches a given keyword.", true));

        toolsMap.put("find_files_by_extension", byExtTool);
        toolsList.add(new ToolInfo("find_files_by_extension", "Find Files by Extension",
                "Search for files on your computer that have a specific file extension (e.g. pdf, txt, png).", true));

        toolsMap.put("system_info", sysInfoTool);
        toolsList.add(new ToolInfo("system_info", "System Info",
                "Retrieve system specifications, memory status, disk space, and user environment.", true));

        toolsMap.put("platform_type", platformTypeTool);
        toolsList.add(new ToolInfo("platform_type", "Platform Type",
                "Identify the platform type of this computer (linux, mac, or windows).", true));

        toolsMap.put("get_home_directory", getHomeDirTool);
        toolsList.add(new ToolInfo("get_home_directory", "Get Home Directory",
                "Retrieve the absolute path to the user's home directory.", true));
                
        toolsMap.put("calculator", calcTool);
        toolsList.add(new ToolInfo("calculator", "Calculator", "Evaluates a mathematical expression.", true));
        
        toolsMap.put("get_current_datetime", dateTimeTool);
        toolsList.add(new ToolInfo("get_current_datetime", "Date & Time", "Gets the current system date and time.", true));
        
        toolsMap.put("get_weather", weatherTool);
        toolsList.add(new ToolInfo("get_weather", "Weather", "Gets the current weather for a specified location.", true));
        
        toolsMap.put("read_file", fileReadTool);
        toolsList.add(new ToolInfo("read_file", "Read File", "Reads the contents of a local file.", true));
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
}

