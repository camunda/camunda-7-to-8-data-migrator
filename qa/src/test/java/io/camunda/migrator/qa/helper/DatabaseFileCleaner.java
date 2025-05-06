/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.qa.helper;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clean the .mv.db files if they're present before running the tests.
 * Files are under /target, so this step has no effect if 'mvn clean' is run before the execution.
 */
@Component
public class DatabaseFileCleaner {

  @Value("${migrator.source.jdbc-url}")
  protected String sourceJdbcUrl;

  @Value("${migrator.target.jdbc-url}")
  protected String targetJdbcUrl;

  protected static final Pattern DB_FILE_URL_PATTERN = Pattern.compile("^jdbc:[^:]+:(?:file:)?([^;]+)");

  @PostConstruct
  public void deleteDatabaseFiles() throws IOException {
     Files.deleteIfExists(extractFilePathFromJdbcUrl(sourceJdbcUrl));
     Files.deleteIfExists(extractFilePathFromJdbcUrl(targetJdbcUrl));
  }

  protected static Path extractFilePathFromJdbcUrl(String jdbcUrl) {
    if (jdbcUrl == null) return null;
    Matcher matcher = DB_FILE_URL_PATTERN.matcher(jdbcUrl);
    String fileName = matcher.find() ? matcher.group(1) : "";
    return Path.of(fileName + ".mv.db");
  }
}