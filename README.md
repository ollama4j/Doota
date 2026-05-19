# Doota

<p align="center">
  <img alt="doota-logo" src="https://github.com/user-attachments/assets/028def1f-7763-4d76-8569-463fe32e85c4" width="250"/>
</p>

<div align="center">
An agentic UI for Ollama. Built with Ollama4J, Quarkus, React and TypeScript.
</div>

<div align="center">
  <img alt="GitHub Release" src="https://img.shields.io/github/v/release/ollama4j/ollama4j-ui">
</div>

## Requirements

<a href="https://ollama.com/" target="_blank">
  <img src="https://img.shields.io/badge/Java-11_+-green.svg?style=for-the-badge&labelColor=gray&label=Java&color=orange" alt=""/>
  <img src="https://img.shields.io/badge/v0.6.2+-green.svg?style=for-the-badge&labelColor=gray&label=Ollama&color=blue" alt=""/>
</a>

## Download and Run


Find the latest release [here](https://github.com/ollama4j/ollama4j-ui/releases/latest) and download the `.jar` file. Then, run the following command:

```bash
java -jar /path/to/ollama4j-ui-<version>.jar
```

#### Chat with model selection

<p align="center">
  <img width="1920" height="1080" alt="Doota Meaning" src="https://github.com/user-attachments/assets/46a6d913-79db-48b3-a1f8-753ffba9ae5c" />
</p>

#### Agentic conversation

<p align="center">
<img width="1080" height="608" alt="Doota Tool Call (2)" src="https://github.com/user-attachments/assets/f25381ba-1200-463d-8814-59710035be84" />
</p>

## Features

### 🤖 Autonomous Agentic Chat Loop
* **Client-Driven Reasoning**: A fully autonomous agentic loop running directly in the browser (supporting up to 10 iterations per request).
* **Dynamic Decision Making**: The LLM autonomously determines when to execute tools, sequences multiple tool calls dynamically, and compiles final answers.
* **Live Status Diagnostics**: Visually track the step-by-step thinking process (e.g., "Thinking...", "Asking the model...", "Executing tool: x...").
* **Performance Metrics**: Real-time generation statistics featuring live tokens-per-second (TPS) metrics.

### 🔌 Interactive Tool Execution & Inspection
* **Live Tool Status**: Status states with beautiful visual cues for Pending (⏳), Running (⚙️), Success (✅), and Error (❌) executions.
* **Collapsible Details**: Accordion cards display parsed JSON arguments, execution time, and raw results for every tool invoked.
* **Error Resilience**: Robust system-level and LLM-level error handling ensuring stable execution of sequential tool logic.

### 🛠️ Built-in Agent Tools (Out-of-the-box)
* **Filesystem & Search**:
  * `find_files_by_name`: Recursively searches files across a chosen directory (defaults to home directory) using keyword/substring search.
  * `find_files_by_extension`: Recursively locates files matching specific file formats (e.g., `.java`, `.pdf`, `.png`).
  * `read_file`: Safe retrieval of text-based file contents.
  * `get_home_directory`: Resolves the absolute path of the user's home directory.
* **System Diagnostics & Environment**:
  * `system_info`: Inspects system hardware details, including OS name/version, CPU cores, JVM memory capacity, and drive storage sizes.
  * `platform_type`: Safely identifies the operating system family (`mac`, `windows`, or `linux`).
* **General Utilities**:
  * `calculator`: Safe mathematical expression evaluator supporting basic and complex arithmetic.
  * `get_current_datetime`: Queries the real-time system date and time.
  * `get_weather`: Location-based weather report function.

### 📊 Advanced Model Management
* **Search, Filter & Sort**: Easily query models via name, family, parameter size, or quantization level. Sort lists by name, file size, or modified date.
* **Detailed Specifications**: Hover and view comprehensive metadata cards detailing model family (e.g., Llama, Mistral, Qwen), parameter sizes, and quantization.
* **Memory Management**: Polls loaded models in real time; allows one-click unloading to release system RAM, or permanent deletion from disk.

### ⚙️ Real-time Settings & Connection Diagnostics
* **Custom Host URL**: Seamlessly configure custom Ollama URLs (e.g., `http://localhost:11434` or remote setups) in a dedicated settings view.
* **Heartbeat & Polling**: Active background pings (every 3 seconds) verify host connection health and show a live server reachability status.
* **Live Polling**: Programmatic loaded-model checks (every 1 second) keep the UI perfectly in sync with Ollama's active memory usage.

### 💾 Local Chat Persistence & UX Polish
* **Local Persistence**: Conversations are automatically persisted as JSON files under your home directory (`~/doota/chats`), preserving full multi-turn context.
* **Premium Theme**: Modern dark-themed user interface styled with premium layouts and powered by smooth hover animations.
* **Vibrant Details**: Clean Markdown rendering (with full GFM support), code highlighting, simple sidebar navigation, and quick copy-to-clipboard buttons.

## ⭐ Give us a Star!

If you like or are using this project to build your own, please give us a star. It's a free way to show your support.

### Appreciate the work?

<p align="center">
  <a href="https://www.buymeacoffee.com/amithkoujalgi" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>
</p>
