#!/usr/bin/env bash

source .env

docker container prune -y

docker run -d \
  --name rabbitmq \
  -e RABBITMQ_DEFAULT_USER=$RABBITMQ_DEFAULT_USER \
  -e RABBITMQ_DEFAULT_PASS=$RABBITMQ_DEFAULT_PASS \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:4.0.7-management

docker run -d \
  --name mysql \
  -e MYSQL_USER=$MYSQL_USER \
  -e MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD \
  -e MYSQL_PASSWORD=$MYSQL_PASSWORD \
  -e MYSQL_HOST=localhost \
  -e MYSQL_DATABASE=$MYSQL_DATABASE \
  -p 3306:3306 \
  mysql:9.2.0

docker run --name redis-cache \
  -p 6379:6379 \
  -v redis-data:/data \
  -d redis:8.0.3-alpine \
  redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru