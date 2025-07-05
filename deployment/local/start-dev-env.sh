#!/bin/bash

# Nimbus Local Development Environment
# Dieses Skript startet alle lokalen Services für die Entwicklung

echo "🚀 Starte Nimbus lokale Entwicklungsumgebung..."

# Services starten
docker-compose up -d

echo "⏳ Warte auf Services..."
sleep 10

# Kafka Topics erstellen
echo "📝 Erstelle Kafka Topics..."
docker exec nimbus-kafka-tools kafka-topics --create --topic lookup --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic lookup-response --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic planet-lookup --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic planet-lookup-response --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic planet-registration --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic planet-registration-response --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic registry-events --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec nimbus-kafka-tools kafka-topics --create --topic dead-letter-queue --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1

echo "✅ Lokale Entwicklungsumgebung gestartet!"
echo ""
echo "📋 Verfügbare Services:"
echo "  🔍 Kafka UI:       http://localhost:8081"
echo "  📊 Kafka:          localhost:9092"
echo "  🏗️  Schema Registry: http://localhost:8082"
echo "  🐘 PostgreSQL:     localhost:5432 (nimbus/nimbus123)"
echo "  🔴 Redis:          localhost:6379"
echo ""
echo "🛠️  Nützliche Befehle:"
echo "  Topics anzeigen:   docker exec nimbus-kafka-tools kafka-topics --list --bootstrap-server kafka:29092"
echo "  Nachrichten senden: docker exec -it nimbus-kafka-tools kafka-console-producer --topic lookup --bootstrap-server kafka:29092"
echo "  Nachrichten lesen:  docker exec -it nimbus-kafka-tools kafka-console-consumer --topic lookup --from-beginning --bootstrap-server kafka:29092"
echo "  Schemas anzeigen:   curl http://localhost:8082/subjects"
echo ""
echo "🔧 Zum Stoppen: ./stop-dev-env.sh"
