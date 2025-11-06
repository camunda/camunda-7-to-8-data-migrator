package io.camunda.migrator.qa.identity;

import io.camunda.client.api.search.enums.PermissionType;
import io.camunda.client.api.search.enums.ResourceType;

public record C8Auth(ResourceType resourceType,
                     String resourceId,
                     Owner owner,
                     PermissionType permission) {
}
