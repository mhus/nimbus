#!/bin/bash

# Stoppt die lokale Entwicklungsumgebung

echo "🛑 Stoppe Nimbus lokale Entwicklungsumgebung..."

docker-compose down

echo "✅ Lokale Entwicklungsumgebung gestoppt!"
echo ""
echo "💡 Zum Löschen aller Daten: docker-compose down -v"
