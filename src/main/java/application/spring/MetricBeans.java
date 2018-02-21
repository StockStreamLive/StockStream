package application.spring;

import network.gateway.aws.MetricPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricBeans {

    @Bean
    public MetricPublisher metricPublisher() {
        return new MetricPublisher();
    }

}
