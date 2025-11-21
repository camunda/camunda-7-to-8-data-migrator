#!/bin/bash
set -e

# Determine which docker-compose file to use
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"

echo "Starting Docker Compose with: $COMPOSE_FILE"

# Start docker compose in the background
docker compose -f "$COMPOSE_FILE" up -d

# If using the data setup, wait for migration to complete
if [ "$COMPOSE_FILE" = "docker-compose-with-data.yml" ]; then
  echo "Waiting for migration to complete..."
  
  # Wait for the migration completion message
  timeout=300  # 5 minutes max wait
  elapsed=0
  
  while [ $elapsed -lt $timeout ]; do
    # Check logs for the completion message
    if docker compose -f "$COMPOSE_FILE" logs data-migrator 2>/dev/null | grep -q "Migration completed - test data ready"; then
      echo "✓ Migration completed! Test data is ready."
      break
    fi
    
    # Check if the container is still running
    if ! docker compose -f "$COMPOSE_FILE" ps data-migrator --format json 2>/dev/null | grep -q "running\|exited"; then
      echo "Warning: data-migrator container not found or not started yet"
    fi
    
    sleep 2
    elapsed=$((elapsed + 2))
    
    if [ $((elapsed % 10)) -eq 0 ]; then
      echo "Still waiting for migration... (${elapsed}s elapsed)"
    fi
  done
  
  if [ $elapsed -ge $timeout ]; then
    echo "ERROR: Timeout waiting for migration to complete"
    docker compose -f "$COMPOSE_FILE" logs data-migrator
    exit 1
  fi
else
  echo "Using basic setup - no migration wait needed"
fi

# Keep the script running to maintain docker compose
echo "Services are ready. Keeping containers running..."
tail -f /dev/null
