This is a distribution of

       Camunda 7 Data Migrator v${project.version}

visit
       http://docs.camunda.org/

==================

Usage

To run the Camunda 7 Data Migrator:

1. Adjust `./configuration/application.yml`  
   - The `source` config should point to your Camunda 7 database  
   - The `target` config should point to your Camunda 8 database

2. Execute the `start` script:  
   - `start.bat` for Windows  
   - `start.sh` for Linux/macOS

Options

- `--runtime` – Migrate runtime data only  
- `--history` – Migrate history data only 

==================

Contents:

  /
      Contains two start scripts: one for Windows (`.bat`) and one for Linux/macOS (`.sh`).

  lib/
      Contains the Java application.

  configuration/
      Contains all configuration resources, including `application.yml`.

  userlib/
      Add custom database drivers, plugins, or other extensions here.


==================

      Camunda 7 Data Migrator v${project.version}

=================
