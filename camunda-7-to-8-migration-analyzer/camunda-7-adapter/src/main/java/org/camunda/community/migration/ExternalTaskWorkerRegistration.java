package org.camunda.community.migration;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1.JobWorkerBuilderStep3;
import io.camunda.zeebe.spring.client.annotation.processor.AbstractZeebeAnnotationProcessor;
import io.camunda.zeebe.spring.client.bean.ClassInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.camunda.bpm.client.spring.impl.client.ClientConfiguration;
import org.camunda.bpm.client.spring.impl.subscription.SpringTopicSubscriptionImpl;
import org.camunda.community.migration.worker.ExternalTaskHandlerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalTaskWorkerRegistration extends AbstractZeebeAnnotationProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(ExternalTaskWorkerRegistration.class);
  private final ClientConfiguration clientConfiguration;
  private final Map<String, SpringTopicSubscriptionImpl> springTopicSubscriptions = new HashMap<>();
  private final List<JobWorker> openedWorkers = new ArrayList<>();

  public ExternalTaskWorkerRegistration(ClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  private Long calculateLockDuration(SpringTopicSubscriptionImpl subscription) {
    Long lockDuration = clientConfiguration.getLockDuration();
    if (subscription.getLockDuration() != null && subscription.getLockDuration() < 0L) {
      lockDuration = subscription.getLockDuration();
    }
    return lockDuration;
  }

  private <T> void setIfPresent(T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }

  @Override
  public boolean isApplicableFor(ClassInfo beanInfo) {
    return SpringTopicSubscriptionImpl.class.isAssignableFrom(beanInfo.getBean().getClass());
  }

  @Override
  public void configureFor(ClassInfo beanInfo) {
    LOG.info("Registering Zeebe worker(s) of bean: {}", beanInfo.getBean());
    springTopicSubscriptions.put(
        beanInfo.getBeanName(), (SpringTopicSubscriptionImpl) beanInfo.getBean());
  }

  @Override
  public void start(ZeebeClient zeebeClient) {
    springTopicSubscriptions.forEach(
        (beanName, bean) -> {
          JobWorkerBuilderStep3 builder =
              zeebeClient
                  .newWorker()
                  .jobType(bean.getTopicName())
                  .handler(
                      new ExternalTaskHandlerWrapper(
                          bean.getExternalTaskHandler(), Optional.empty()))
                  .name(beanName);
          setIfPresent(calculateLockDuration(bean), builder::timeout);
          setIfPresent(clientConfiguration.getMaxTasks(), builder::maxJobsActive);
          setIfPresent(
              clientConfiguration.getAsyncResponseTimeout(),
              timeout -> builder.pollInterval(Duration.ofMillis(timeout)));
          setIfPresent(bean.getVariableNames(), builder::fetchVariables);
          setIfPresent(
              clientConfiguration.getAsyncResponseTimeout(),
              timeout -> builder.requestTimeout(Duration.ofMillis(timeout)));
          openedWorkers.add(builder.open());
        });
  }

  @Override
  public void stop(ZeebeClient zeebeClient) {
    openedWorkers.forEach(JobWorker::close);
  }
}
