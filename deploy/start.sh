#!/bin/bash

set -e

cd /home/ec2-user/backend

echo "Starting backend container..."

docker run -d \
  --name backend \
  --restart unless-stopped \
  -p 127.0.0.1:8080:8080 \
  -v /home/ec2-user/backend/config/application.properties:/config/application.properties:ro \
  backend-app

echo "Backend started."

docker ps