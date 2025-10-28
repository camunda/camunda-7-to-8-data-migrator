/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter.visitor.impl.dmn;

import org.camunda.community.migration.converter.DomElementVisitorContext;
import org.camunda.community.migration.converter.convertible.TextConvertible;
import org.camunda.community.migration.converter.expression.ExpressionTransformationResult;
import org.camunda.community.migration.converter.expression.ExpressionTransformationResultMessageFactory;
import org.camunda.community.migration.converter.expression.ExpressionTransformer;
import org.camunda.community.migration.converter.visitor.AbstractDmnElementVisitor;

public class TextVisitor extends AbstractDmnElementVisitor {

  @Override
  public String localName() {
    return "text";
  }

  @Override
  protected void visitDmnElement(DomElementVisitorContext context) {
    // ignore text annotations
    if (hasTextAnnotationParent(context)) {
      return;
    }
    String content = context.getElement().getTextContent();
    ExpressionTransformationResult transform =
        ExpressionTransformer.transformToFeelDmn("Text field", content);
    context.addConversion(TextConvertible.class, c -> c.setContent(transform.result()));
    context.addMessage(ExpressionTransformationResultMessageFactory.getMessage(transform, null));
  }

  private boolean hasTextAnnotationParent(DomElementVisitorContext context) {
    return context.getElement().getParentElement().getLocalName().equals("textAnnotation");
  }
}
