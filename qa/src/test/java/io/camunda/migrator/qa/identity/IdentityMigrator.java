package io.camunda.migrator.qa.identity;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.search.enums.PermissionType;
import io.camunda.client.api.search.enums.ResourceType;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.authorization.Permissions;
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
    var c7Permissions = authorizationManager.getC7Permissions()
        .stream()
        .filter(auth ->  auth.resourceType().equals(Resources.TENANT) || auth.resourceType().equals(Resources.TENANT_MEMBERSHIP)) // Testing purposes
        .toList();

    // map permissions to C8
    var c8Permissions = c7Permissions.stream()
        .flatMap( c7Auth -> {
          Pair<ResourceType, Set<PermissionType>> c8PermMapping = AuthMapper.mapAuthorization(c7Auth.resourceType(), c7Auth.permission());
          return c8PermMapping.getValue().stream().map(perm -> new C8Auth(
              c8PermMapping.getKey(),
              c7Auth.resourceId(), // TODO: This also need to be mapped. For example, in C7 the ID might be xId but in C8 it will be xKey
              c7Auth.owner(),
              perm));
        })
        .distinct()
        .toList();

    // create permissions
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
}
