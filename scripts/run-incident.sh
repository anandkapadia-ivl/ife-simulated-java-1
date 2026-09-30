#!/usr/bin/env bash
set -e

for i in $(seq 1 15); do
  curl -s -X POST "http://localhost:8080/incident/memory-leak?megabytes=10" >/dev/null
done

curl -s -X POST "http://localhost:8080/incident/waiting-threads?count=40&waitMs=300000" >/dev/null

for i in $(seq 1 20); do
  curl -s -X POST "http://localhost:8080/incident/db-contention?sleepMs=30000" >/dev/null &
done

echo "Incident workload started. Capture jstack/jmap now."
