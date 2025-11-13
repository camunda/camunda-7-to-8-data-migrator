package io.camunda.migrator.qa.identity;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.PermissionType;
import io.camunda.client.api.search.enums.ResourceType;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.authorization.Resources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentityMigrator {

  @Autowired
  protected CamundaClient camundaClient;

  @Autowired
  protected AuthorizationManager authorizationManager;

  @Autowired
  protected IdentityManager identityManager;

  public void migrate() {
    // retrieve permissions
    var c7Authorizations = getC7Authorizations();

    // validate permissions

    // map permissions to C8
    var c8Permissions = mapC8Authorizations(c7Authorizations);

    // create permissions
    migratePermissions(c8Permissions);
  }

  private void migratePermissions(List<C8Auth> c8Permissions) {
    c8Permissions.forEach(c8Auth -> {
      if (!identityManager.ownerExists(c8Auth.owner())){
        System.out.println(String.format("Cannot migrate, owner does not exist in C8: %s", c8Auth.owner()));
      } else if (authorizationManager.permissionExistsInC8(c8Auth)) {
        System.out.println(String.format("Cannot migrate, permission already exists in C8: %s", c8Auth));
      } else {
        camundaClient.newCreateAuthorizationCommand()
          .ownerId(c8Auth.owner().ownerId())
          .ownerType(c8Auth.owner().ownerType())
          .resourceId(c8Auth.resourceId())
          .resourceType(c8Auth.resourceType())
          .permissionTypes(c8Auth.permission())
          .execute();
        System.out.println(String.format("Permission successfully migrated: %s", c8Auth));
      }
    });
  }

  private static List<C8Auth> mapC8Authorizations(List<C7Auth> c7Authorizations) {
    return c7Authorizations.stream()
        .flatMap( c7Auth -> {
          Pair<ResourceType, Set<PermissionType>> c8PermMapping = AuthMapper.mapAuthorization(c7Auth.resourceType(), c7Auth.permission());
          return c8PermMapping.getValue().stream().map(perm -> new C8Auth(
              c8PermMapping.getKey(),
              c7Auth.resourceId(), // TODO: This also need to be mapped depending on the resource type, see https://docs.camunda.io/docs/next/components/concepts/access-control/authorizations/#available-resources
              c7Auth.owner(),
              perm));
        })
        .distinct()
        .toList();
  }

  private List<C7Auth> getC7Authorizations() {
    return authorizationManager.getC7Permissions()
        .stream()
        .filter(auth ->  auth.resourceType().equals(Resources.TENANT) || auth.resourceType().equals(Resources.TENANT_MEMBERSHIP)) // Testing purposes, to be removed
        .toList();
  }
}
