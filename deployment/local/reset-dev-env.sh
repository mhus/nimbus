#!/bin/bash

# Setzt die lokale Entwicklungsumgebung zurück (löscht alle Daten)

echo "🔄 Setze Nimbus lokale Entwicklungsumgebung zurück..."

docker-compose down -v
docker-compose pull

echo "✅ Entwicklungsumgebung zurückgesetzt!"
echo ""
echo "🚀 Zum Neustarten: ./start-dev-env.sh"
