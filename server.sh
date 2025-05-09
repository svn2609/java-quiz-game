#!/bin/bash
echo "🔨 Compiling SERVER source files..."
javac -d bin -cp "lib/sqlite-jdbc-3.49.1.0.jar" src/**/*.java

echo "🚀 Starting Quiz Server..."
java -cp "bin:lib/sqlite-jdbc-3.49.1.0.jar" server.QuizServer
