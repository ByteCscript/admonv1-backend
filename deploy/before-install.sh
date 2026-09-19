#!/bin/bash

set -e

echo "Stopping existing backend container..."

if docker ps -q -f name=backend | grep -q .; then
    docker stop backend
fi

if docker ps -aq -f name=backend | grep -q .; then
    docker rm backend
fi

if docker images -q backend-app | grep -q .; then
    docker rmi backend-app || true
fi

echo "Existing backend stopped."