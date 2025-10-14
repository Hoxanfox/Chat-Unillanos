#!/bin/bash

echo "========================================"
echo "  INICIANDO CLIENTE CHAT-UNILLANOS"
echo "========================================"
echo ""

# Ir al directorio del script
cd "$(dirname "$0")"

echo "Compilando el cliente..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: No se pudo compilar el cliente"
    exit 1
fi

echo ""
echo "Iniciando la aplicación JavaFX..."
echo ""

cd Presentacion/Main
mvn javafx:run

