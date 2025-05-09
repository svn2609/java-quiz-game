#!/bin/bash
echo "🔨 Compiling ADMIN source files..."
javac -d bin -cp "lib/sqlite-jdbc-3.49.1.0.jar" src/**/*.java

echo "🛠️ Launching Admin Client..."
java -cp "bin:lib/sqlite-jdbc-3.49.1.0.jar" client.AdminClient
