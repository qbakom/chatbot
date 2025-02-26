# Chatbot Console Application

This project is a console application designed for interacting with a chatbot using LM Studio. It supports both C and Java programming languages.

## Features

- Console-based interaction with the chatbot.
- Integration with LM Studio for chatbot functionality.
- Custom extensions.
- Capability to maintain conversation context.

### Prerequisites

- LM Studio
- C Compiler
- Java JDK

### Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/qbakom/chatbot.git
   cd chatbot

2. Build the C code:
    ```sh
    gcc -o chatbot main.c -lcurl -lcjson

3. Compile the Java code:
    ```sh
    javac -d bin src/*.java