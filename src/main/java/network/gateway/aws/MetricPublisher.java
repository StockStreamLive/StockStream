package network.gateway.aws;

import application.AWSConfig;
import application.Config;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class MetricPublisher {

    private static final String METRIC_NAMESPACE = "StockStream";

    final AmazonCloudWatchAsync amazonCloudWatchAsync =
            AmazonCloudWatchAsyncClientBuilder.standard().withRegion(AWSConfig.AWS_REGION).withCredentials(AWSConfig.PROVIDER).build();

    public void publishMetric(final String metricName, final double metricValue) {
        Dimension dimension = new Dimension().withName("Stage").withValue(Config.stage.name());

        MetricDatum datum = new MetricDatum().withMetricName(metricName)
                                             .withUnit(StandardUnit.None)
                                             .withValue(metricValue)
                                             .withDimensions(dimension);

        PutMetricDataRequest request = new PutMetricDataRequest().withNamespace(METRIC_NAMESPACE)
                                                                 .withMetricData(datum);

        amazonCloudWatchAsync.putMetricData(request);
    }
}
