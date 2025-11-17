#!/bin/bash

# TypeScript to Java Generator Script
# This script generates Java classes from TypeScript interfaces and enums
# 
# Usage: ./generate-java-from-typescript.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "TypeScript to Java Generator"
echo "============================"
echo ""

# Define source directories and target packages
TS_TYPES_DIR="client/packages/shared/src/types"
TS_MESSAGES_DIR="client/packages/shared/src/network/messages"
TS_CONSTANTS_DIR="client/packages/shared/src/constants"
TS_REST_DIR="client/packages/shared/src/rest"
BASE_PACKAGE="de.mhus.nimbus.generated"
JAVA_GENERATED_ROOT="$PROJECT_ROOT/server/generated/src/main/java/de/mhus/nimbus/generated"

# Create output directories if they don't exist
mkdir -p "$JAVA_GENERATED_ROOT/types"
mkdir -p "$JAVA_GENERATED_ROOT/network"
mkdir -p "$JAVA_GENERATED_ROOT/constants"
mkdir -p "$JAVA_GENERATED_ROOT/rest"

# Clean existing generated files
echo "Cleaning existing generated files..."
rm -f "$JAVA_GENERATED_ROOT/types"/*.java
rm -f "$JAVA_GENERATED_ROOT/network"/*.java
rm -f "$JAVA_GENERATED_ROOT/constants"/*.java
rm -f "$JAVA_GENERATED_ROOT/rest"/*.java
echo ""

# Check if Node.js is available
NODE_CMD=""
if command -v node &> /dev/null; then
    NODE_CMD="node"
elif [ -f "/opt/homebrew/bin/node" ]; then
    NODE_CMD="/opt/homebrew/bin/node"
elif [ -f "/usr/local/bin/node" ]; then
    NODE_CMD="/usr/local/bin/node"
fi

if [ -n "$NODE_CMD" ]; then
    echo "Using Node.js generator ($NODE_CMD)..."
    echo ""
    
    # Generate types package
    echo "Generating types from: $TS_TYPES_DIR"
    echo "Target package: $BASE_PACKAGE.types"
    "$NODE_CMD" "$SCRIPT_DIR/ts-to-java-generator.js" "$TS_TYPES_DIR" "$BASE_PACKAGE" "types"
    echo ""
    
    # Generate network base types (MessageTypes.ts, etc.)
    echo "Generating network base types from: client/packages/shared/src/network"
    echo "Target package: $BASE_PACKAGE.network"
    "$NODE_CMD" "$SCRIPT_DIR/ts-to-java-generator.js" "client/packages/shared/src/network" "$BASE_PACKAGE" "network"
    echo ""
    
    # Generate network messages
    echo "Generating network messages from: $TS_MESSAGES_DIR"
    echo "Target package: $BASE_PACKAGE.network"
    "$NODE_CMD" "$SCRIPT_DIR/ts-to-java-generator.js" "$TS_MESSAGES_DIR" "$BASE_PACKAGE" "network"
    echo ""
    
    # Generate constants
    echo "Generating constants from: $TS_CONSTANTS_DIR"
    echo "Target package: $BASE_PACKAGE.constants"
    "$NODE_CMD" "$SCRIPT_DIR/ts-to-java-generator.js" "$TS_CONSTANTS_DIR" "$BASE_PACKAGE" "constants"
    echo ""
    
    # Generate rest DTOs
    echo "Generating REST DTOs from: $TS_REST_DIR"
    echo "Target package: $BASE_PACKAGE.rest"
    "$NODE_CMD" "$SCRIPT_DIR/ts-to-java-generator.js" "$TS_REST_DIR" "$BASE_PACKAGE" "rest"
    echo ""
else
    echo "Node.js not found. Please install Node.js to use the automatic generator."
    echo ""
    echo "Alternative: You can manually create Java classes based on the TypeScript definitions."
    echo ""
    echo "Or install Node.js and run this script again."
    exit 1
fi

echo ""
echo "✓ Generation complete!"
echo ""
echo "Generated Java classes are in:"
echo "  Types:     $JAVA_GENERATED_ROOT/types"
echo "  Network:   $JAVA_GENERATED_ROOT/network"
echo "  Constants: $JAVA_GENERATED_ROOT/constants"
echo "  REST:      $JAVA_GENERATED_ROOT/rest"
echo ""
echo "To regenerate, run this script again:"
echo "  $SCRIPT_DIR/generate-java-from-typescript.sh"
