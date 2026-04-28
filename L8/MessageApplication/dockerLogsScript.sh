#!/bin/bash

id=$(docker ps --filter ancestor=$1 --format "{{.ID}}")
docker logs "$id"
