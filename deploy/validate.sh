#!/bin/bash

set -e

echo "Validating backend..."

for i in {1..30}; do
    if curl -f http://127.0.0.1:8080/api/hola > /dev/null; then
        echo "Backend validation successful."
        exit 0
    fi

    echo "Waiting for backend..."
    sleep 1
done

echo "Backend validation failed."
docker logs backend
exit 1