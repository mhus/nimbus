#!/bin/bash

# TypeScript to Java Generator Script
# This script generates Java classes from TypeScript interfaces and enums
# 
# Usage: ./generate-java-from-typescript.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TS_SOURCE_DIR="$PROJECT_ROOT/client/packages/shared/src/types"
JAVA_OUTPUT_DIR="$PROJECT_ROOT/server/generated/src/main/java/de/mhus/nimbus/generated"

echo "TypeScript to Java Generator"
echo "============================"
echo ""
echo "Source: $TS_SOURCE_DIR"
echo "Output: $JAVA_OUTPUT_DIR"
echo ""

# Create output directory if it doesn't exist
mkdir -p "$JAVA_OUTPUT_DIR"

# Clean existing generated files
echo "Cleaning existing generated files..."
rm -f "$JAVA_OUTPUT_DIR"/*.java

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
    "$NODE_CMD" "$SCRIPT_DIR/ts-to-java-generator.js"
else
    echo "Node.js not found. Please install Node.js to use the automatic generator."
    echo ""
    echo "Alternative: You can manually create Java classes based on the TypeScript definitions in:"
    echo "  $TS_SOURCE_DIR"
    echo ""
    echo "Or install Node.js and run this script again."
    exit 1
fi

echo ""
echo "✓ Generation complete!"
echo ""
echo "Generated Java classes are in:"
echo "  $JAVA_OUTPUT_DIR"
echo ""
echo "To regenerate, run this script again:"
echo "  $SCRIPT_DIR/generate-java-from-typescript.sh"
