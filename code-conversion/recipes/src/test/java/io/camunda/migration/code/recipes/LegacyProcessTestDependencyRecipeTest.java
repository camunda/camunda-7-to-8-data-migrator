/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migration.code.recipes;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class LegacyProcessTestDependencyRecipeTest implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipeFromResources(
        "io.camunda.migration.code.recipes.sharedRecipes.MigrateLegacyProcessTestDependenciesRecipe");
  }

  @Test
  void migratesLegacyMavenProcessTestDependencies() {
    rewriteRun(
        pomXml(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-tests</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>spring-boot-starter-camunda-test-testcontainer</artifactId>
                  <version>8.9.0</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>zeebe-process-test-extension</artifactId>
                  <version>8.9.0</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-tests</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-spring-boot-3</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-java</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """));
  }

  @Test
  void migratesLegacyGradleProcessTestDependencies() {
    rewriteRun(
        buildGradle(
            """
            plugins {
                id 'java'
            }

            dependencies {
                testImplementation "io.camunda:spring-boot-starter-camunda-test:8.9.0"
                testImplementation "io.camunda:zeebe-process-test-extension-testcontainer:8.9.0"
            }
            """,
            """
            plugins {
                id 'java'
            }

            dependencies {
                testImplementation "io.camunda:camunda-process-test-spring-boot-3:8.10.0-SNAPSHOT"
                testImplementation "io.camunda:camunda-process-test-java:8.10.0-SNAPSHOT"
            }
            """));
  }

  @Test
  void migratesTemporarySpringBoot4ProcessTestArtifacts() {
    rewriteRun(
        pomXml(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-tests</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-spring-4</artifactId>
                  <version>8.8.21</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-tests</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-spring</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """));

    rewriteRun(
        buildGradle(
            """
            plugins {
                id 'java'
            }

            dependencies {
                testImplementation "io.camunda:camunda-process-test-spring-boot-4:8.8.22"
            }
            """,
            """
            plugins {
                id 'java'
            }

            dependencies {
                testImplementation "io.camunda:camunda-process-test-spring:8.10.0-SNAPSHOT"
            }
            """));
  }

  @Test
  void migratesLegacyJavaClientDependencies() {
    rewriteRun(
        spec ->
            spec.recipeFromResources(
                "io.camunda.migration.code.recipes.sharedRecipes.MigrateLegacyJavaClientDependencyRecipe"),
        pomXml(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-client</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>zeebe-client-java</artifactId>
                  <version>8.9.0</version>
                </dependency>
              </dependencies>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-client</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-client-java</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                </dependency>
              </dependencies>
            </project>
            """));

    rewriteRun(
        spec ->
            spec.recipeFromResources(
                "io.camunda.migration.code.recipes.sharedRecipes.MigrateLegacyJavaClientDependencyRecipe"),
        buildGradle(
            """
            plugins {
                id 'java'
            }

            dependencies {
                implementation "io.camunda:zeebe-client-java:8.9.0"
            }
            """,
            """
            plugins {
                id 'java'
            }

            dependencies {
                implementation "io.camunda:camunda-client-java:8.10.0-SNAPSHOT"
            }
            """));
  }

  @Test
  void removesDuplicateMavenClientTargetAfterMigration() {
    rewriteRun(
        spec ->
            spec.recipeFromResources(
                "io.camunda.migration.code.recipes.sharedRecipes.MigrateLegacyJavaClientDependencyRecipe"),
        pomXml(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-client</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>zeebe-client-java</artifactId>
                  <version>8.9.0</version>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-client-java</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                </dependency>
              </dependencies>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-client</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-client-java</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                </dependency>
              </dependencies>
            </project>
            """));
  }

  @Test
  void removesDuplicateMavenTargetsAfterMigration() {
    rewriteRun(
        pomXml(
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-tests</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>spring-boot-starter-camunda-test</artifactId>
                  <version>8.9.0</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-spring-boot-3</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>zeebe-process-test-extension</artifactId>
                  <version>8.9.0</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-java</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>legacy-tests</artifactId>
              <version>1.0.0</version>
              <dependencies>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-spring-boot-3</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
                <dependency>
                  <groupId>io.camunda</groupId>
                  <artifactId>camunda-process-test-java</artifactId>
                  <version>8.10.0-SNAPSHOT</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """));
  }
}
