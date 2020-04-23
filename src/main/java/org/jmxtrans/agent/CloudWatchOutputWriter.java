package org.jmxtrans.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.jmxtrans.agent.util.ConfigurationUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

public class CloudWatchOutputWriter extends AbstractOutputWriter {

  private CloudWatchAsyncClient client;
  private String configNamespace;
  private String configDimensions;
  private List<Tag> listOfDimensions;
  private Collection<Dimension> dimensions;

  @Override
  public void postConstruct(Map<String, String> settings) {

    client = CloudWatchAsyncClient.create();
    configNamespace = ConfigurationUtils.getString(settings, "namespace", "JMX");
    configDimensions = ConfigurationUtils.getString(settings, "dimensions", "");
    listOfDimensions = Tag.tagsFromCommaSeparatedString(configDimensions);

    dimensions = new ArrayList<Dimension>();

    for (Tag thisDimension : listOfDimensions) {
      dimensions.add(
          Dimension.builder()
              .name(thisDimension.getName())
              .value(thisDimension.getValue())
              .build());
    }
  }

  @Override
  public void writeQueryResult(String name, String type, Object value) {

    Double doubleValue;

    if (value instanceof Number) {
      Number numberValue = (Number) value;
      doubleValue = numberValue.doubleValue();
    } else {
      logger.log(Level.WARNING, "Cannot write result " + name + ". " + value + " is not a Number.");
      return;
    }

    try {

      MetricDatum datum =
          MetricDatum.builder().metricName(name).value(doubleValue).dimensions(dimensions).build();

      PutMetricDataRequest request =
          PutMetricDataRequest.builder().namespace(configNamespace).metricData(datum).build();

      client.putMetricData(request);

    } catch (CloudWatchException e) {
      logger.log(Level.SEVERE, e.awsErrorDetails().errorMessage());
    }
  }

  @Override
  public void writeInvocationResult(String invocationName, Object value) throws IOException {
    writeQueryResult(invocationName, null, value);
  }

  @Override
  public void preDestroy() {
    super.preDestroy();
    client.close();
  }
}
