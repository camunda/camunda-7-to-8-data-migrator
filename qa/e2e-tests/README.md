# E2E Tests for Cockpit Plugin

This directory contains end-to-end tests for the Camunda 7 to 8 Data Migrator Cockpit Plugin using Playwright.

## Overview

These tests validate that:
- Camunda 7 starts successfully with the plugin deployed
- The Cockpit UI is accessible
- The plugin UI is visible on the processes dashboard
- The plugin can interact with the UI to display migrated/skipped entities

## Prerequisites

- Node.js 18+ and npm
- Docker and Docker Compose
- Built Cockpit plugin JAR (`plugins/cockpit/target/camunda-7-to-8-data-migrator-cockpit-plugin-0.2.0-SNAPSHOT.jar`)

## Setup

1. **Build the Cockpit plugin first:**
   ```bash
   cd ../../plugins/cockpit
   mvn clean install
   cd ../../qa/e2e-tests
   ```

2. **Install dependencies:**
   ```bash
   npm install
   npx playwright install chromium
   ```

## Running Tests

### Run all tests (basic mode - fast)
```bash
npm test
# or explicitly
npm run test:basic
```

### Run tests with real migration data
```bash
npm run test:with-data
```

### Run tests with UI mode (interactive)
```bash
npm run test:ui
```

### Run tests in headed mode (see the browser)
```bash
npm run test:headed
```

### Run tests in debug mode
```bash
npm run test:debug
```

## How It Works

### Basic Mode (Default)
1. **Docker Compose** starts a Camunda 7 instance with the plugin JAR mounted
2. **Playwright** waits for Camunda to be ready (health check)
3. **Tests** navigate to the Cockpit, login, and interact with the plugin UI
4. **Screenshots** are captured for verification and debugging

**Usage:** `npm test` or `npm run test:basic`

### With Real Demo Data
To test with real migrated data, use the enhanced setup:

```bash
npm run test:with-data
```

**What happens:**
1. The `start-services.sh` script starts the full Docker Compose stack:
   - **Camunda 7** (with PostgreSQL) - source system
   - **Zeebe/C8** (with Elasticsearch) - target system
   - **Data Migrator** - runs migration to populate test data
   - **Cockpit Plugin** - mounted with real migration_mapping table data
2. Script **waits for the migration to complete** by monitoring logs for "Migration completed - test data ready"
3. Once ready, Playwright tests run against the populated database
4. Tests validate plugin behavior with actual migrated/skipped entities

**Manual approach:**
```bash
# Use the full stack with Camunda 7, Zeebe, and Data Migrator
COMPOSE_FILE=docker-compose-with-data.yml bash start-services.sh &

# Wait for migration message in logs, then:
npm test

# Cleanup
docker compose -f docker-compose-with-data.yml down -v
```

The `docker-compose-with-data.yml` setup includes:
- **Camunda 7** (with PostgreSQL) - source system
- **Zeebe/C8** (with Elasticsearch) - target system
- **Data Migrator** - runs migration to populate test data
- **Cockpit Plugin** - mounted with real migration_mapping table data

This allows testing with actual migrated process instances instead of empty tables.

## Test Structure

- `docker-compose.yml` - Simple Camunda 7 instance with plugin (default, fast)
- `docker-compose-with-data.yml` - Full stack with real migration data (comprehensive testing)
- `playwright.config.ts` - Playwright configuration with webServer setup
- `tests/cockpit-plugin.spec.ts` - Main E2E test suite

## Test Coverage

The test suite includes:
- ✅ Camunda Cockpit loads successfully
- ✅ Plugin UI appears on the processes page
- ✅ Migrated and skipped entity tabs are visible
- ✅ Entity type selector works
- ✅ Empty state is displayed correctly
- ✅ No JavaScript/React errors in console

## Troubleshooting

### Tests fail with "Connection refused"
- Ensure Docker is running
- Check that port 8080 is not already in use
- Increase the timeout in `playwright.config.ts`

### Plugin not found error
- Verify the plugin JAR exists at `../../plugins/cockpit/target/`
- Build the plugin with `mvn clean install -pl plugins/cockpit`

### Camunda takes too long to start
- Increase `webServer.timeout` in `playwright.config.ts`
- Check Docker logs: `docker compose logs -f`

### docker-compose command not found
- The tests use Docker Compose V2 (`docker compose` with a space)
- If you have the older standalone `docker-compose`, update `playwright.config.ts` to use `docker-compose up`

## CI/CD Integration

The E2E tests run automatically in CI on every pull request and push to main.

The CI job:
1. Builds the Cockpit plugin JAR
2. Installs Node.js and npm dependencies
3. Installs Playwright browsers
4. Starts Camunda 7 with Docker Compose
5. Runs the E2E test suite
6. Uploads test reports and screenshots as artifacts

To run in CI:
```bash
# Set CI environment variable
CI=true npm test
```

This enables:
- Retries on failure
- Parallel execution disabled
- GitHub Actions annotations for test results
- Better error reporting

## Screenshots

Test screenshots are saved to `test-results/` directory for debugging.
