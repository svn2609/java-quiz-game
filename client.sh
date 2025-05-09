#!/bin/bash
echo "🔨 Compiling CLIENT source files..."
javac -d bin -cp "lib/sqlite-jdbc-3.49.1.0.jar" src/**/*.java

echo "🧑‍💻 Opening 3 Quiz Clients in separate terminal tabs..."

for i in 1 2 3
do
  osascript -e "tell application \"Terminal\" to do script \"cd $(pwd); java -cp 'bin:lib/sqlite-jdbc-3.49.1.0.jar' client.QuizGUI\""
  sleep 1
done
