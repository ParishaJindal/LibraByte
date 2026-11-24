#!/usr/bin/env bash
# ==============================================================================
# LibraByte - Backend Compilation Script
# Compiles all Core Java source files into backend/bin/
# ==============================================================================

set -e

# Detect Java compiler
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
elif [ -x "/opt/homebrew/opt/openjdk/bin/javac" ]; then
    JAVAC="/opt/homebrew/opt/openjdk/bin/javac"
else
    JAVAC="javac"
fi

echo "============================================="
echo " Compiling LibraByte Core Java Backend..."
echo " Using compiler: $($JAVAC -version 2>&1)"
echo "============================================="

mkdir -p backend/bin backend/database

find backend/src -name "*.java" > sources.txt
$JAVAC -cp "backend/lib/sqlite-jdbc-3.48.0.0.jar:backend/src" -d backend/bin @sources.txt
rm sources.txt

echo "Compilation successful! Output directory: backend/bin/"
