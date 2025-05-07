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
        The root directory contains two start scripts. One for Windows (.bat)
        and one for Linux/Mac (.sh). After executing it, you can access the 
        following web applications:

        webapps: http://localhost:8080/
        rest: http://localhost:8080/engine-rest/

  internal/
        This directory contains the Java application and optional components
        that Camunda Platform Run consists of.

  configuration/
        This directory contains all resources to configure the distro.
        Find a detailed guide on how to use this directory on the following
        documentation pages:
        https://docs.camunda.org/manual/latest/installation/camunda-bpm-run/
        https://docs.camunda.org/manual/latest/user-guide/camunda-bpm-run/

==================

      Camunda 7 Data Migrator v${project.version}

=================
