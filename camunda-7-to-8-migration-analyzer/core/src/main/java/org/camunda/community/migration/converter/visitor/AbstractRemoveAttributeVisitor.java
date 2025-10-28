/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter.visitor;

import org.camunda.community.migration.converter.DomElementVisitorContext;
import org.camunda.community.migration.converter.message.MessageFactory;

public abstract class AbstractRemoveAttributeVisitor extends AbstractCamundaAttributeVisitor {

  @Override
  protected void visitAttribute(DomElementVisitorContext context, String attribute) {
    context.addMessage(
        MessageFactory.attributeRemoved(attributeLocalName(), context.getElement().getLocalName()));
  }
}
