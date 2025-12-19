#!/bin/bash
set -e

# export BP_PLATFORM_VARIANTS="linux/amd64,linux/arm64"

mvn -Pnative spring-boot:build-image
