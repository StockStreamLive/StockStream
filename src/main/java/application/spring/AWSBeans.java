package application.spring;

import network.gateway.aws.OrderPublisher;
import network.gateway.aws.RoundPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSBeans {

    @Bean
    public OrderPublisher orderPublisher() {
        return new OrderPublisher();
    }

    @Bean
    public RoundPublisher roundPublisher() {
        return new RoundPublisher();
    }

}
