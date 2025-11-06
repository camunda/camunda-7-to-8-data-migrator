package io.camunda.migrator.qa.identity;

import io.camunda.client.api.search.enums.OwnerType;

public record Owner(OwnerType ownerType, String ownerId) {

}