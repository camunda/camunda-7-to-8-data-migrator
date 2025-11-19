/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import { test, expect } from '@playwright/test';

/**
 * E2E smoke test for the Cockpit plugin
 * 
 * This test validates that:
 * 1. Camunda 7 starts successfully with the plugin deployed
 * 2. The Cockpit UI is accessible
 * 3. The plugin UI is visible on the processes dashboard
 * 4. The plugin can interact with migrated/skipped entities
 */

test.describe('Cockpit Plugin E2E', () => {
  
  test.beforeEach(async ({ page }) => {
    // Navigate to Camunda Cockpit and login
    await page.goto('/camunda/app/cockpit/default/');
    
    // Login with default credentials
    await page.fill('input[name="username"]', 'demo');
    await page.fill('input[name="password"]', 'demo');
    await page.click('button[type="submit"]');
    
    // Wait for the dashboard to load
    await page.waitForURL('**/cockpit/default/#/dashboard');
  });

  test('should load Camunda Cockpit successfully', async ({ page }) => {
    // Verify we're on the Cockpit dashboard
    await expect(page).toHaveURL(/.*cockpit.*dashboard/);
    
    // Verify the page title
    await expect(page).toHaveTitle(/Cockpit/);
  });

  test('should display the migrator plugin on processes page', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for the plugin to load - look for the plugin title
    const pluginTitle = page.locator('h1.section-title:has-text("Camunda 7 to 8 Data Migrator")');
    await pluginTitle.waitFor({ timeout: 10000 });
    
    // Verify the plugin title is visible
    await expect(pluginTitle).toBeVisible();
    
    // Take a screenshot for verification
    await page.screenshot({ path: 'test-results/plugin-on-processes-page.png', fullPage: true });
  });

  test('should display migrated and skipped entity tabs', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for the plugin to render
    await page.waitForTimeout(2000); // Give React time to render
    
    // Look for the radio buttons for skipped/migrated
    const skippedRadio = page.locator('input[type="radio"][value="skipped"]');
    const migratedRadio = page.locator('input[type="radio"][value="migrated"]');
    
    // Verify both radio buttons are visible
    await expect(skippedRadio).toBeVisible();
    await expect(migratedRadio).toBeVisible();
    
    // Verify the labels are present
    await expect(page.locator('text=Skipped')).toBeVisible();
    await expect(page.locator('text=Migrated')).toBeVisible();
    
    // Take a screenshot
    await page.screenshot({ path: 'test-results/plugin-tabs.png', fullPage: true });
  });

  test('should be able to switch between entity types', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for plugin to load
    await page.waitForTimeout(2000);
    
    // Switch to History mode to access the entity type selector
    const historyRadio = page.locator('input[type="radio"][value="history"]');
    await historyRadio.click();
    
    // Wait for the dropdown to appear
    await page.waitForTimeout(500);
    
    // Look for the entity type selector dropdown
    const entityTypeSelector = page.locator('select#type-selector');
    
    // Verify the selector is visible
    await expect(entityTypeSelector).toBeVisible();
    
    // Take screenshot of the dropdown
    await page.screenshot({ path: 'test-results/entity-type-selector.png', fullPage: true });
    
    // Verify we can see options
    const options = entityTypeSelector.locator('option');
    const optionsCount = await options.count();
    
    expect(optionsCount).toBeGreaterThan(0);
  });

  test('should display empty state when no entities exist', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for plugin to load
    await page.waitForTimeout(2000);
    
    // Since this is a fresh Camunda instance with no migrations run,
    // the table should be empty or show "No data" / count of 0
    
    // Look for the table or empty state indicators
    // The PaginatedTable component should render something even when empty
    const tableElement = page.locator('table');
    
    // Take screenshot of empty state
    await page.screenshot({ path: 'test-results/empty-state.png', fullPage: true });
    
    // Verify either the table exists (possibly empty) or we see a loading/empty message
    const hasTable = await tableElement.isVisible().catch(() => false);
    const hasLoading = await page.locator('text=Loading').isVisible().catch(() => false);
    const hasNoData = await page.locator('text=/No|Empty|0/i').first().isVisible().catch(() => false);
    
    // At least one of these should be true
    expect(hasTable || hasLoading || hasNoData).toBeTruthy();
  });

  test('should render plugin UI elements without errors', async ({ page }) => {
    // Listen for console errors before navigation
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });
    
    // Navigate to processes page  
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for plugin to fully render
    await page.locator('h1:has-text("Camunda 7 to 8 Data Migrator")').waitFor({ timeout: 10000 });
    await page.waitForTimeout(2000);
    
    // Take final screenshot
    await page.screenshot({ path: 'test-results/plugin-loaded.png', fullPage: true });
    
    // Verify no React errors or critical JavaScript errors
    const hasReactErrors = errors.some(err => 
      err.includes('React') || 
      err.includes('TypeError') ||
      err.includes('ReferenceError') ||
      err.includes('is not a function')
    );
    
    // Log errors for debugging if any exist
    if (errors.length > 0) {
      console.log('Console errors detected:', errors);
    }
    
    expect(hasReactErrors).toBeFalsy();
  });
});
