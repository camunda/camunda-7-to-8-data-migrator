package io.camunda.migrator.history;

public interface IdKeyMapper {

  String findLatestIdByType(String type);

  Long findKeyById(String id);

  void insert(IdKeyDbModel idKeyDbModel);

}
