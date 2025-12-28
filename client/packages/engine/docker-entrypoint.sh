#!/bin/sh
set -e

# Docker entrypoint script for nimbus-engine
# Generates config.json from environment variables at runtime

CONFIG_FILE="/usr/share/nginx/html/config.json"

# Default values
API_URL="${VITE_SERVER_API_URL:-http://localhost:9042}"

echo "Generating runtime configuration for Nimbus Engine..."
echo "API_URL: $API_URL"

# Generate config.json
cat > "$CONFIG_FILE" <<EOF
{
  "apiUrl": "$API_URL"
}
EOF

echo "Runtime configuration generated at $CONFIG_FILE"
cat "$CONFIG_FILE"

# Note:
# - worldId is passed via URL parameter (?worldId=xxx)
# - websocketUrl and exitUrl come from server config (/player/world/config)

# Execute the main container command (typically nginx)
exec "$@"
