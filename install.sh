#!/usr/bin/env bash

set -e

echo "========================================="
echo "           Installing Doota              "
echo "========================================="

# 1. Check dependencies
if ! command -v curl &> /dev/null; then
    echo "Error: curl is required but not installed."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "Error: Java (17+) is required but not installed."
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# 2. Setup directories
DOOTA_DIR="$HOME/.doota"
BIN_DIR="$HOME/.local/bin"
JAR_PATH="$DOOTA_DIR/doota-runner.jar"

mkdir -p "$DOOTA_DIR"
mkdir -p "$BIN_DIR"

# 3. Fetch latest release info
echo "Fetching latest release information..."
LATEST_RELEASE_API="https://api.github.com/repos/ollama4j/Doota/releases/latest"

DOWNLOAD_URL=$(curl -s $LATEST_RELEASE_API | grep -o 'https://github.com/ollama4j/Doota/releases/download/[^"]*-runner.jar' | head -n 1)

if [ -z "$DOWNLOAD_URL" ]; then
    echo "Error: Failed to find the latest Doota release jar."
    echo "Please check the GitHub releases page: https://github.com/ollama4j/Doota/releases"
    exit 1
fi

# 4. Download JAR
echo "Downloading Doota from $DOWNLOAD_URL ..."
curl -# -L -o "$JAR_PATH" "$DOWNLOAD_URL"

# 5. Create launcher script
SCRIPT_PATH="$BIN_DIR/doota"
cat << EOF > "$SCRIPT_PATH"
#!/usr/bin/env bash
java -jar "$JAR_PATH" "\$@"
EOF

chmod +x "$SCRIPT_PATH"

echo "========================================="
echo "Doota has been installed successfully! 🎉"
echo "========================================="
echo ""
echo "The executable 'doota' is located at: $SCRIPT_PATH"
echo ""

if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    echo "⚠️  NOTE: $BIN_DIR is not in your PATH."
    echo "To run 'doota' from anywhere, add this line to your ~/.bashrc, ~/.zshrc, or profile:"
    echo "    export PATH=\"\$HOME/.local/bin:\$PATH\""
    echo ""
    echo "Then restart your terminal or run:"
    echo "    source ~/.bashrc (or ~/.zshrc)"
    echo ""
    echo "In the meantime, you can start Doota by running:"
    echo "    $SCRIPT_PATH"
else
    echo "You can now start the application by running:"
    echo "    doota"
fi
