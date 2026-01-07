#!/bin/bash

set -e

cd $(dirname "$0")
./build-local.sh

cd local-all
docker compose up -d
docker compose logs --tail 10 -f world-player world-control world-generator
