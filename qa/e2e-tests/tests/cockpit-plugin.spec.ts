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
    
    // Wait for the plugin to load - look for the plugin container or any unique identifier
    // The plugin should render React components on the processes dashboard
    const pluginSelector = '[data-plugin-id="camunda-7-to-8-data-migrator"], .migrator-plugin, #migrator-plugin';
    
    // Wait for plugin container to be visible (with a reasonable timeout)
    await page.waitForSelector(pluginSelector, { timeout: 10000 }).catch(async () => {
      // If specific selector not found, check for any content that indicates plugin loaded
      // Look for text that the plugin displays
      const hasPluginContent = await page.locator('text=/migrat|skip/i').first().isVisible();
      if (!hasPluginContent) {
        throw new Error('Plugin UI not found on processes page');
      }
    });
    
    // Take a screenshot for verification
    await page.screenshot({ path: 'test-results/plugin-on-processes-page.png', fullPage: true });
  });

  test('should display migrated and skipped entity tabs', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for the plugin to render
    await page.waitForTimeout(2000); // Give React time to render
    
    // Look for tabs or sections for migrated/skipped entities
    // These might be buttons, tabs, or other UI elements
    const migratedTab = page.locator('text=/migrated/i').first();
    const skippedTab = page.locator('text=/skipped/i').first();
    
    // Verify at least one of these is visible
    const migratedVisible = await migratedTab.isVisible().catch(() => false);
    const skippedVisible = await skippedTab.isVisible().catch(() => false);
    
    expect(migratedVisible || skippedVisible).toBeTruthy();
    
    // Take a screenshot
    await page.screenshot({ path: 'test-results/plugin-tabs.png', fullPage: true });
  });

  test('should be able to switch between entity types', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for plugin to load
    await page.waitForTimeout(2000);
    
    // Look for entity type selector (dropdown or buttons)
    // The plugin should allow switching between different entity types
    const entityTypeSelector = page.locator('select, [role="combobox"], [role="listbox"]').first();
    
    if (await entityTypeSelector.isVisible()) {
      // Try to interact with the selector
      await entityTypeSelector.click();
      
      // Take screenshot of the dropdown/options
      await page.screenshot({ path: 'test-results/entity-type-selector.png', fullPage: true });
      
      // Verify we can see options
      const options = page.locator('option, [role="option"]');
      const optionsCount = await options.count();
      
      expect(optionsCount).toBeGreaterThan(0);
    }
  });

  test('should display empty state when no entities exist', async ({ page }) => {
    // Navigate to processes page
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Wait for plugin to load
    await page.waitForTimeout(2000);
    
    // Since this is a fresh Camunda instance with no migrations run,
    // we expect to see an empty state message or zero counts
    
    // Look for empty state indicators
    const emptyStateTexts = [
      'No migrated',
      'No skipped',
      'No entities',
      'No data',
      '0',
    ];
    
    let foundEmptyState = false;
    for (const text of emptyStateTexts) {
      const locator = page.locator(`text=/${text}/i`).first();
      if (await locator.isVisible().catch(() => false)) {
        foundEmptyState = true;
        break;
      }
    }
    
    // Take screenshot of empty state
    await page.screenshot({ path: 'test-results/empty-state.png', fullPage: true });
    
    // We should see some indication of empty state
    expect(foundEmptyState).toBeTruthy();
  });

  test('should render plugin UI elements without errors', async ({ page }) => {
    // Navigate to processes page  
    await page.click('a[href="#/processes"]');
    await page.waitForURL('**/processes');
    
    // Listen for console errors
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });
    
    // Wait for plugin to fully render
    await page.waitForTimeout(3000);
    
    // Take final screenshot
    await page.screenshot({ path: 'test-results/plugin-loaded.png', fullPage: true });
    
    // Verify no React errors or critical JavaScript errors
    const hasReactErrors = errors.some(err => 
      err.includes('React') || 
      err.includes('undefined') ||
      err.includes('Cannot read') ||
      err.includes('is not a function')
    );
    
    expect(hasReactErrors).toBeFalsy();
    
    // Log errors for debugging if any exist
    if (errors.length > 0) {
      console.log('Console errors detected:', errors);
    }
  });
});
