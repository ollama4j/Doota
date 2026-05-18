package io.github.ollama4j.api;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.tools.Tools;
import io.github.ollama4j.api.tools.FindFilesByNameToolFunction;
import io.github.ollama4j.api.tools.FindFilesByExtensionToolFunction;
import io.github.ollama4j.api.tools.SystemInfoToolFunction;
import io.github.ollama4j.api.tools.PlatformTypeToolFunction;
import io.github.ollama4j.api.tools.GetHomeDirectoryToolFunction;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class OllamaService {
    private Ollama ollama;
    private String host = "http://localhost:11434";

    private final List<ToolInfo> toolsList = new ArrayList<>();
    private final Map<String, Tools.Tool> toolsMap = new HashMap<>();

    public OllamaService() {
        initOllama();
        initTools();
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
    }

    public void deleteModel(String modelName) throws Exception {
        ollama.deleteModel(modelName, true);
    }
}

