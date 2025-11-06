package io.camunda.migrator.qa.identity;

import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Resource;

public record C7Auth(Resource resourceType,
                     String resourceId,
                     Owner owner,
                     Permission permission) {
}
