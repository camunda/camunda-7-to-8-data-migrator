/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.impl.util;

import io.camunda.migrator.config.property.MigratorProperties;
import io.camunda.migrator.impl.clients.C7Client;
import io.camunda.migrator.impl.model.FlowNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;

public class C7Utils {

  public static final String SUB_PROCESS_ACTIVITY_TYPE = "subProcess";
  public static final String MULTI_INSTANCE_BODY_SUFFIX = "#multiInstanceBody";
  public static final String PARALLEL_GATEWAY_ACTIVITY_TYPE = "parallelGateway";

  /**
   * Collects active activity IDs by recursively traversing the activity instance tree.
   */
  public static Map<String, FlowNode> getActiveActivityIdsById(ActivityInstance activityInstance,
                                                               Map<String, FlowNode> activeActivities,
                                                               MigratorProperties migratorProperties,
                                                               C7Client c7Client) {
    Arrays.asList(activityInstance.getChildActivityInstances()).forEach(actInst -> {
      activeActivities.putAll(getActiveActivityIdsById(actInst, activeActivities, migratorProperties, c7Client));

      if (!SUB_PROCESS_ACTIVITY_TYPE.equals(actInst.getActivityType()) && !PARALLEL_GATEWAY_ACTIVITY_TYPE.equals(actInst.getActivityType())) {
        activeActivities.put(actInst.getId(),
            new FlowNode(actInst.getActivityId(), ((ActivityInstanceImpl) actInst).getSubProcessInstanceId(), actInst.getActivityType()));
      }
      if (PARALLEL_GATEWAY_ACTIVITY_TYPE.equals(actInst.getActivityType())) {
        switch(migratorProperties.getMergingGateWayStrategy()) {
          case IGNORE -> {
            // do nothing, the parallel gateway will be ignored by C8 when the next activities are activated
            // will not progress through the gateway even if all currently active branches are completed
          }
          case ACTIVATE_LAST_ACTIVITIES -> {
            // Dynamically find completed activities before this parallel gateway using C7Client
            List<HistoricActivityInstance> completedActivitiesBeforeGateway =
                c7Client.getCompletedActivitiesBeforeGateway(activityInstance, actInst.getActivityId());

            // Reactivate the most recently completed activities from each parallel branch
            completedActivitiesBeforeGateway.forEach(historicActivity -> {
              activeActivities.put(historicActivity.getId(),
                  new FlowNode(historicActivity.getActivityId(), historicActivity.getCalledProcessInstanceId(),
                      historicActivity.getActivityType()));
            });
          }
          case MIGRATE, SKIP -> {
            // MIGRATE: existing BUG behavior that makes the flow continue without waiting in front of the gateway
            // SKIP: will be handled in the RuntimeValidator
            activeActivities.put(actInst.getId(),
                new FlowNode(actInst.getActivityId(), ((ActivityInstanceImpl) actInst).getSubProcessInstanceId(), actInst.getActivityType()));
          }
        }
      }
    });

    /* TODO: Transition instances might map to start before or after.
    When it maps to asyncBefore it should be fine. When it maps to asyncAfter an execution is fired twice in C7 and C8.
     */
    Arrays.asList(activityInstance.getChildTransitionInstances()).forEach(ti -> {
      var transitionInstance = ((TransitionInstanceImpl) ti);
      if (!SUB_PROCESS_ACTIVITY_TYPE.equals(transitionInstance.getActivityType())) {
        activeActivities.put(transitionInstance.getId(),
            new FlowNode(transitionInstance.getActivityId(), transitionInstance.getSubProcessInstanceId(), null));
      }
    });
    return activeActivities;
  }

}
