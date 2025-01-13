//package io.camunda.migrator;
//
//import io.camunda.db.rdbms.write.RdbmsWriter;
//import io.camunda.db.rdbms.write.RdbmsWriterFactory;
//import io.camunda.db.rdbms.write.domain.ProcessInstanceDbModel;
//import io.camunda.db.rdbms.write.service.ProcessInstanceWriter;
//import org.apache.ibatis.session.Configuration;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.apache.ibatis.session.SqlSessionFactoryBuilder;
//import org.camunda.bpm.engine.ProcessEngine;
//import org.camunda.bpm.engine.ProcessEngineConfiguration;
//import org.camunda.bpm.engine.history.HistoricProcessInstance;
//import org.camunda.bpm.engine.HistoryService;
//import io.camunda.db.rdbms.RdbmsService;
//
//import java.util.List;
//
//public class CamundaMigrator {
//
//
//
//  public static void main(String[] args) {
//    // Configure and build the C7 process engine
//    ProcessEngine c7Engine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
//        .setJdbcUrl("jdbc:h2:tcp://localhost/~/camunda7")
//        .setJdbcUsername("sa")
//        .setJdbcPassword("")
//        .setJdbcDriver("org.h2.Driver")
//        .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
//        .buildProcessEngine();
//
//    // Configure and build the C8 process engine
//    RdbmsService c8Service = new RdbmsService();
//    RdbmsWriterFactory rdbmsWriterFactory = new RdbmsWriterFactory();
//
//    // Deploy process definition to C7
//
//    // Produce history data on C7
//    c7Engine.getRuntimeService().startProcessInstanceById("Process_07e8as6");
//
//    // Deploy process definition to C8
//    // Todo how to transform the C7 process to C8.
//
//    // Migrate history data from C7 to C8
//    migrateHistoricProcessInstances(c7Engine, c8Service);
//
//    // Close C7 engine
//    c7Engine.close();
//  }
//
//  private static void migrateHistoricProcessInstances(ProcessEngine c7Engine, RdbmsService c8Service) {
//    HistoryService c7HistoryService = c7Engine.getHistoryService();
//    List<HistoricProcessInstance> instances = c7HistoryService.createHistoricProcessInstanceQuery().list();
//
//    for (HistoricProcessInstance instance : instances) {
//      // Migrate the process instance to C8
//      migrateProcessInstance(instance);
//    }
//  }
//
//  private static void migrateProcessInstance(RdbmsService c8Service, HistoricProcessInstance instance) {
//    // C8 RDBMS ProcessInstanceWriter to persist the data to the database
//
//    //TODO how should these values be populated?
//    long partitionId = 1L; // zeebee partition id
//    int queueSize = 1; // the size of flushing, set to 1 to start small
//
//    RdbmsWriter rdbmsWriter = c8Service.createWriter(partitionId, queueSize);
//
//    ProcessInstanceWriter processInstanceWriter = rdbmsWriter.getProcessInstanceWriter();
//
//    ProcessInstanceDbModel processInstance = ProcessInstanceDbModelFactory.create(instance);
//
//    processInstanceWriter.create(processInstance);
//  }
//
//}
