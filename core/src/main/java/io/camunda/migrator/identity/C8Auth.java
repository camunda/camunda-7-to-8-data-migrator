package io.camunda.migrator.identity;

import io.camunda.client.api.search.enums.PermissionType;
import io.camunda.client.api.search.enums.ResourceType;

public record C8Auth(ResourceType resourceType,
                     String resourceId,
                     Owner owner,
                     PermissionType permission) {
}
