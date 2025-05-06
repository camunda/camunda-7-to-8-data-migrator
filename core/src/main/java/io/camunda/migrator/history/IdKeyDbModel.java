/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.migrator.history;

import java.util.Objects;

public class IdKeyDbModel {

  protected Long key;
  protected String id;
  protected String type;

  public Long key() {
    return key;
  }

  public String id() {
    return id;
  }

  public String type() {
    return type;
  }

  public void setKey(Long key) {
    this.key = key;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    var that = (IdKeyDbModel) obj;
    return Objects.equals(this.key, that.key) && Objects.equals(this.id, that.id) && Objects.equals(this.type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, id, type);
  }

  @Override
  public String toString() {
    return "IdKey[" + "key=" + key + ", " + "id=" + id + ", " + "type=" + type + ']';
  }

}
