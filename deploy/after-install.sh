#!/bin/bash

set -e

cd /home/ec2-user/backend

echo "Building Docker image..."

docker build -t backend-app .

echo "Docker image built successfully."