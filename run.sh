#!/usr/bin/env bash
# ==============================================================================
# LibraByte - Application Launch Script
# Starts the pure Core Java HTTP REST API & Static File Web Server on port 8080
# ==============================================================================

set -e

PORT=${1:-8080}

# Detect Java runtime
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif [ -x "/opt/homebrew/opt/openjdk/bin/java" ]; then
    JAVA="/opt/homebrew/opt/openjdk/bin/java"
else
    JAVA="java"
fi

# Recompile if backend/bin is missing
if [ ! -d "backend/bin" ]; then
    echo "Compiled binaries not found. Running ./compile.sh..."
    ./compile.sh
fi

echo "=========================================================="
echo " Starting LibraByte Web Application on port $PORT..."
echo "=========================================================="
$JAVA --enable-native-access=ALL-UNNAMED -cp "backend/bin:backend/lib/sqlite-jdbc-3.48.0.0.jar" Main "$PORT"
