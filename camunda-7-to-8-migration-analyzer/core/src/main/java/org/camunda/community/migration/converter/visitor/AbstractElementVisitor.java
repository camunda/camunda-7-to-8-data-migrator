/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter.visitor;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.community.migration.converter.DomElementVisitorContext;
import org.camunda.community.migration.converter.message.Message;
import org.camunda.community.migration.converter.message.MessageFactory;
import org.camunda.community.migration.converter.version.SemanticVersion;

public abstract class AbstractElementVisitor extends AbstractFilteringVisitor {

  @Override
  protected boolean canVisit(DomElementVisitorContext context) {
    return namespaceUri().contains(context.getElement().getNamespaceURI())
        && localName().equals(context.getElement().getLocalName());
  }

  protected abstract List<String> namespaceUri();

  public abstract String localName();

  @Override
  protected final void visitFilteredElement(DomElementVisitorContext context) {
    visitElement(context);
    SemanticVersion availableFrom = availableFrom(context);
    if (availableFrom == null) {
      context.addMessage(cannotBeConvertedMessage(context));
    } else if (isNotSupportedInDesiredVersion(
        availableFrom, SemanticVersion.parse(context.getProperties().getPlatformVersion()))) {
      context.addMessage(supportedInFutureVersionMessage(context, availableFrom));
    }
  }

  protected final boolean isNotSupportedInDesiredVersion(
      SemanticVersion availableFrom, SemanticVersion desiredVersion) {
    return availableFrom.ordinal() > desiredVersion.ordinal();
  }

  protected abstract SemanticVersion availableFrom(DomElementVisitorContext context);

  protected abstract void visitElement(DomElementVisitorContext context);

  protected Message supportedInFutureVersionMessage(
      DomElementVisitorContext context, SemanticVersion availableFrom) {
    return MessageFactory.elementAvailableInFutureVersion(
        elementNameForMessage(context.getElement()),
        context.getProperties().getPlatformVersion(),
        availableFrom.getPatchZeroVersion());
  }

  protected Message cannotBeConvertedMessage(DomElementVisitorContext context) {
    return MessageFactory.elementNotSupportedHint(
        elementNameForMessage(context.getElement()), context.getProperties().getPlatformVersion());
  }

  protected String elementNameForMessage(DomElement element) {
    return StringUtils.capitalize(element.getLocalName().replaceAll("([A-Z])", " $1"));
  }

  protected boolean isOnBpmnElement(
      DomElementVisitorContext context, String namespaceUri, String bpmnElementLocalName) {
    DomElement element = context.getElement();
    while (!(element.getLocalName().equals(bpmnElementLocalName)
        && element.getNamespaceURI().equals(namespaceUri))) {
      element = element.getParentElement();
      if (element == null) {
        return false;
      }
    }
    return true;
  }
}
