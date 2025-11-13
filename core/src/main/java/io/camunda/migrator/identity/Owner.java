package io.camunda.migrator.identity;

import io.camunda.client.api.search.enums.OwnerType;

public record Owner(OwnerType ownerType, String ownerId) {

}