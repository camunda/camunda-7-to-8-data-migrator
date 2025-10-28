/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter.visitor;

import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.community.migration.converter.DomElementVisitorContext;

public abstract class AbstractEventRefVisitor extends AbstractBpmnAttributeVisitor {

  @Override
  protected void visitAttribute(DomElementVisitorContext context, String attribute) {
    context.references(attribute);
  }

  protected boolean isEndEvent(DomElement element) {
    return element.getParentElement().getLocalName().equals("endEvent");
  }

  protected boolean isIntermediateThrowEvent(DomElement element) {
    return element.getParentElement().getLocalName().equals("intermediateThrowEvent");
  }
}
