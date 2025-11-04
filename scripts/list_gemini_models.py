#!/usr/bin/env python3
"""
List available Gemini models to find the correct model name.
"""

import os
import sys

try:
    import google.generativeai as genai
except ImportError:
    print("Error: google-generativeai not installed")
    print("Install with: pip install google-generativeai")
    sys.exit(1)

# Get API key
api_key = os.environ.get('GOOGLE_API_KEY')
if not api_key:
    print("Error: GOOGLE_API_KEY environment variable not set")
    sys.exit(1)

# Configure
genai.configure(api_key=api_key)

print("Available Gemini models:\n")

# List all models
for model in genai.list_models():
    if 'generateContent' in model.supported_generation_methods:
        print(f"✓ {model.name}")
        print(f"  Display Name: {model.display_name}")
        print(f"  Description: {model.description}")
        print(f"  Methods: {', '.join(model.supported_generation_methods)}")
        print()
