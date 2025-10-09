#!/bin/bash

echo "========================================"
echo "  INICIANDO SERVIDOR CHAT-UNILLANOS"
echo "========================================"
echo ""

# Ir al directorio del script
cd "$(dirname "$0")"

echo "Compilando el servidor..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: No se pudo compilar el servidor"
    exit 1
fi

echo ""
echo "Iniciando el servidor en el puerto 8888..."
echo ""

mvn exec:java

