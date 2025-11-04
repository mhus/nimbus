#!/usr/bin/env python3
"""
Generate .info files for image assets in the Assets directory.

Analyzes images and creates JSON metadata files with:
- width, height
- dominant color (RGB hex)
- AI-generated description (using Gemini API)

Usage:
    python generate_asset_info.py [--assets-dir PATH] [--api-key KEY] [--overwrite]

Environment variables:
    GOOGLE_API_KEY: Gemini API key (optional if --api-key is provided)
"""

import os
import sys
import json
import argparse
import time
from pathlib import Path
from typing import Dict, Tuple, Optional
import base64

try:
    from PIL import Image
    import numpy as np
except ImportError:
    print("Error: Required packages not installed.")
    print("Please install: pip install Pillow numpy google-generativeai")
    sys.exit(1)

try:
    import google.generativeai as genai
    HAS_GEMINI = True
except ImportError:
    HAS_GEMINI = False
    print("Warning: google-generativeai package not installed. AI descriptions will be skipped.")
    print("Install with: pip install google-generativeai")


# Supported image extensions
IMAGE_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp'}


def get_dominant_color(image_path: str) -> str:
    """
    Get the dominant color from an image.
    Returns RGB hex color code.
    """
    try:
        img = Image.open(image_path)

        # Convert to RGB if necessary
        if img.mode != 'RGB':
            img = img.convert('RGB')

        # Resize for faster processing
        img_small = img.resize((150, 150))

        # Convert to numpy array
        pixels = np.array(img_small)

        # Reshape to list of pixels
        pixels = pixels.reshape(-1, 3)

        # Calculate average color (simple approach)
        # For more sophisticated: could use clustering (k-means)
        avg_color = pixels.mean(axis=0).astype(int)

        # Convert to hex
        hex_color = '#{:02x}{:02x}{:02x}'.format(avg_color[0], avg_color[1], avg_color[2])

        return hex_color
    except Exception as e:
        print(f"  Error getting color: {e}")
        return "#000000"


def get_image_dimensions(image_path: str) -> Tuple[int, int]:
    """
    Get image width and height.
    Returns (width, height) tuple.
    """
    try:
        img = Image.open(image_path)
        return img.size
    except Exception as e:
        print(f"  Error getting dimensions: {e}")
        return (0, 0)


def generate_ai_description(image_path: str, api_key: str, retry_count: int = 0) -> Optional[str]:
    """
    Generate AI description using Gemini API.
    Includes retry logic for rate limits.
    """
    if not HAS_GEMINI:
        return None

    try:
        # Configure Gemini
        genai.configure(api_key=api_key)

        # Try multiple model names (in order of preference)
        model_names = [
            'gemini-2.0-flash-exp',
            'gemini-1.5-flash-latest',
            'gemini-1.5-flash',
            'gemini-1.5-flash-001',
        ]

        model = None
        last_error = None

        for model_name in model_names:
            try:
                model = genai.GenerativeModel(model_name)
                break  # Success, use this model
            except Exception as e:
                last_error = e
                continue

        if model is None:
            raise Exception(f"No available Gemini model found. Last error: {last_error}")

        # Load image
        img = Image.open(image_path)

        # Get filename for context
        filename = Path(image_path).name

        # Create prompt
        prompt = f"Describe this image briefly in 1-2 sentences. This is a texture/asset file named '{filename}'. Focus on what the image shows and its visual characteristics."

        # Generate description
        response = model.generate_content([prompt, img])

        # Extract description
        if response.text:
            description = response.text.strip()
            return description

        return None
    except Exception as e:
        error_str = str(e)

        # Check for rate limit error (429)
        if '429' in error_str or 'quota' in error_str.lower() or 'rate limit' in error_str.lower():
            if retry_count < 3:
                # Extract retry delay if available, otherwise use 30 seconds
                retry_delay = 30
                if 'retry_delay' in error_str:
                    try:
                        # Try to extract seconds from error message
                        import re
                        match = re.search(r'seconds:\s*(\d+)', error_str)
                        if match:
                            retry_delay = int(match.group(1)) + 5  # Add 5 seconds buffer
                    except:
                        pass

                print(f"  Rate limit hit. Waiting {retry_delay} seconds before retry...")
                time.sleep(retry_delay)
                return generate_ai_description(image_path, api_key, retry_count + 1)
            else:
                print(f"  Rate limit error after {retry_count} retries: {e}")
                return None
        else:
            print(f"  Error generating AI description: {e}")
            return None


def analyze_image(image_path: str, api_key: Optional[str] = None) -> Dict:
    """
    Analyze image and return metadata dict.
    """
    print(f"Analyzing: {image_path}")

    # Get dimensions
    width, height = get_image_dimensions(image_path)
    print(f"  Dimensions: {width}x{height}")

    # Get dominant color
    color = get_dominant_color(image_path)
    print(f"  Color: {color}")

    # Generate AI description
    description = ""
    if api_key and HAS_GEMINI:
        print(f"  Generating AI description...")
        ai_desc = generate_ai_description(image_path, api_key)
        if ai_desc:
            description = ai_desc
            print(f"  Description: {description[:60]}...")
        else:
            print(f"  Failed to generate description")
    else:
        print(f"  Skipping AI description (no API key)")

    # Build metadata
    metadata = {
        "description": description,
        "width": width,
        "height": height,
        "color": color,
    }

    return metadata


def find_images(assets_dir: str) -> list:
    """
    Recursively find all images in assets directory.
    """
    images = []
    assets_path = Path(assets_dir)

    for root, dirs, files in os.walk(assets_path):
        for file in files:
            ext = Path(file).suffix.lower()
            if ext in IMAGE_EXTENSIONS:
                # Skip .info files
                if file.endswith('.info'):
                    continue

                full_path = Path(root) / file
                images.append(str(full_path))

    return images


def save_info_file(image_path: str, metadata: Dict) -> None:
    """
    Save metadata to .info file next to the image.
    """
    info_path = f"{image_path}.info"

    try:
        with open(info_path, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
        print(f"  Saved: {info_path}")
    except Exception as e:
        print(f"  Error saving info file: {e}")


def main():
    parser = argparse.ArgumentParser(
        description='Generate .info files for image assets'
    )
    parser.add_argument(
        '--assets-dir',
        type=str,
        default='./client/packages/server/files/assets',
        help='Path to assets directory (default: ./client/packages/server/files/assets)'
    )
    parser.add_argument(
        '--api-key',
        type=str,
        help='Gemini API key (or set GOOGLE_API_KEY env variable)'
    )
    parser.add_argument(
        '--overwrite',
        action='store_true',
        help='Overwrite existing .info files'
    )
    parser.add_argument(
        '--update-empty',
        action='store_true',
        help='Only update .info files where description is empty'
    )
    parser.add_argument(
        '--skip-ai',
        action='store_true',
        help='Skip AI description generation'
    )
    parser.add_argument(
        '--delay',
        type=float,
        default=6.5,
        help='Delay in seconds between API calls (default: 6.5s for ~9 req/min)'
    )

    args = parser.parse_args()

    # Get API key
    api_key = args.api_key or os.environ.get('GOOGLE_API_KEY')

    if not api_key and not args.skip_ai:
        print("Warning: No API key provided. AI descriptions will be skipped.")
        print("Provide --api-key or set GOOGLE_API_KEY environment variable.")
        print("Or use --skip-ai to skip AI descriptions explicitly.\n")

    # Check assets directory
    assets_dir = Path(args.assets_dir)
    if not assets_dir.exists():
        print(f"Error: Assets directory not found: {assets_dir}")
        sys.exit(1)

    print(f"Assets directory: {assets_dir}")
    print(f"Mode: {'Update empty descriptions only' if args.update_empty else 'Overwrite all' if args.overwrite else 'Skip existing'}")
    print(f"AI descriptions: {'Enabled' if api_key and not args.skip_ai else 'Disabled'}")
    if api_key and not args.skip_ai:
        print(f"Delay between API calls: {args.delay}s (~{int(60/args.delay)} req/min)")
    print()

    # Find all images
    images = find_images(str(assets_dir))
    print(f"Found {len(images)} images\n")

    if not images:
        print("No images found.")
        return

    # Process each image
    processed = 0
    skipped = 0

    for image_path in images:
        info_path = f"{image_path}.info"

        # Check if .info already exists
        if Path(info_path).exists():
            if args.update_empty:
                # Check if description is empty
                try:
                    with open(info_path, 'r', encoding='utf-8') as f:
                        existing_info = json.load(f)
                        if existing_info.get('description', '').strip():
                            # Description exists, skip
                            print(f"Skipping (has description): {image_path}")
                            skipped += 1
                            continue
                        else:
                            print(f"Updating (empty description): {image_path}")
                except Exception as e:
                    print(f"Error reading existing info: {e}")
                    # Continue processing
            elif not args.overwrite:
                # Not overwriting and not update-empty mode
                print(f"Skipping (info exists): {image_path}")
                skipped += 1
                continue

        # Analyze image
        try:
            metadata = analyze_image(
                image_path,
                api_key if not args.skip_ai else None
            )

            # Save info file
            save_info_file(image_path, metadata)

            processed += 1

            # Add delay between API calls to avoid rate limits
            if api_key and not args.skip_ai and processed < len(images):
                time.sleep(args.delay)

            print()
        except Exception as e:
            print(f"Error processing {image_path}: {e}\n")

    print(f"\nCompleted!")
    print(f"  Processed: {processed}")
    print(f"  Skipped: {skipped}")
    print(f"  Total: {len(images)}")


if __name__ == '__main__':
    main()
