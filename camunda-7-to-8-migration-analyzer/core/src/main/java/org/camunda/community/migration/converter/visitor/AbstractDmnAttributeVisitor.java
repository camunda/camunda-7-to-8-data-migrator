/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter.visitor;

import static org.camunda.community.migration.converter.NamespaceUri.*;

import java.util.Arrays;
import java.util.List;
import org.camunda.community.migration.converter.DomElementVisitorContext;

public abstract class AbstractDmnAttributeVisitor extends AbstractAttributeVisitor {
  @Override
  protected List<String> namespaceUri() {
    return Arrays.asList(DMN);
  }

  @Override
  protected boolean removeAttribute(DomElementVisitorContext context) {
    return false;
  }
}
