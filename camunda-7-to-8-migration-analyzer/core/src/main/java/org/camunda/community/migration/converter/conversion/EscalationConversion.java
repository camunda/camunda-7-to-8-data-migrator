/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package org.camunda.community.migration.converter.conversion;

import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.community.migration.converter.NamespaceUri;
import org.camunda.community.migration.converter.convertible.EscalationConvertible;

public class EscalationConversion extends AbstractTypedConversion<EscalationConvertible> {
  @Override
  protected Class<EscalationConvertible> type() {
    return EscalationConvertible.class;
  }

  @Override
  protected void convertTyped(DomElement element, EscalationConvertible convertible) {
    element.setAttribute(NamespaceUri.BPMN, "escalationCode", convertible.getEscalationCode());
  }
}
