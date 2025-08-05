/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.bpm.cockpit.plugin.sample.resources;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.camunda.bpm.cockpit.plugin.resource.AbstractCockpitPluginRootResource;
import org.camunda.bpm.cockpit.plugin.sample.SamplePlugin;

/**
 *
 * @author nico.rehwaldt
 */
@Path("plugin/" + SamplePlugin.ID)
public class SamplePluginRootResource extends AbstractCockpitPluginRootResource {

  public SamplePluginRootResource() {
    super(SamplePlugin.ID);
  }

  @Path("{engineName}/process-instance")
  public ProcessInstanceResource getProcessInstanceResource(@PathParam("engineName") String engineName) {
    return subResource(new ProcessInstanceResource(engineName), engineName);
  }
}
