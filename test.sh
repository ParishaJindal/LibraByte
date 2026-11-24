#!/usr/bin/env bash
# ==============================================================================
# LibraByte - Automated 12-Scenario Test Suite
# Tests all functional requirements for college project evaluation
# ==============================================================================

set -e

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif [ -x "/opt/homebrew/opt/openjdk/bin/java" ]; then
    JAVA="/opt/homebrew/opt/openjdk/bin/java"
else
    JAVA="java"
fi

if [ ! -f "backend/bin/TestScenarios.class" ]; then
    ./compile.sh
fi

$JAVA --enable-native-access=ALL-UNNAMED -cp "backend/bin:backend/lib/sqlite-jdbc-3.48.0.0.jar" TestScenarios
