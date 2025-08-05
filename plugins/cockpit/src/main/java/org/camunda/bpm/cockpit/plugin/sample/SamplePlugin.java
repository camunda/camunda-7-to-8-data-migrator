/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.bpm.cockpit.plugin.sample;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.camunda.bpm.cockpit.plugin.sample.resources.SamplePluginRootResource;
import org.camunda.bpm.cockpit.plugin.spi.impl.AbstractCockpitPlugin;

/**
 *
 * @author nico.rehwaldt
 */
public class SamplePlugin extends AbstractCockpitPlugin {

  public static final String ID = "sample-plugin";

  public String getId() {
    return ID;
  }

  @Override
  public Set<Class<?>> getResourceClasses() {
    Set<Class<?>> classes = new HashSet<>();

    classes.add(SamplePluginRootResource.class);

    return classes;
  }

  @Override
  public List<String> getMappingFiles() {
    return Arrays.asList("mapper/IdKey.xml", "mapper/Commons.xml");
  }
}
