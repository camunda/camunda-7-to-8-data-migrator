package io.camunda.migrator.qa;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DatabaseFileCleaner {

  @Value("${migrator.source.jdbc-url}")
  protected String sourceJdbcUrl;

  @Value("${migrator.target.jdbc-url}")
  protected String targetJdbcUrl;

  protected static final Pattern H2_FILE_URL_PATTERN = Pattern.compile("^jdbc:[^:]+:(?:file:)?([^;]+)");

  @PostConstruct
  public void deleteDatabaseFiles() throws IOException {
     Files.deleteIfExists(extractFilePathFromJdbcUrl(sourceJdbcUrl));
     Files.deleteIfExists(extractFilePathFromJdbcUrl(targetJdbcUrl));
  }

  protected static Path extractFilePathFromJdbcUrl(String jdbcUrl) {
    if (jdbcUrl == null) return null;
    Matcher matcher = H2_FILE_URL_PATTERN.matcher(jdbcUrl);
    String fileName = matcher.find() ? matcher.group(1) : "";
    return Path.of(fileName + ".mv.db");
  }
}