#!/bin/bash
# Helper script to run asset info generator with venv

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Check if venv exists, create if not
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
    echo "Installing dependencies..."
    source venv/bin/activate
    pip install -r scripts/requirements.txt
else
    source venv/bin/activate
fi

# Run the script with all passed arguments
echo "Running asset info generator..."
python scripts/generate_asset_info.py "$@"

deactivate
