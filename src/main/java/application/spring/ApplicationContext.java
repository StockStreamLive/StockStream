package application.spring;

import cache.LastOrderCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import stockstream.spring.*;

@Import({LogicBeans.class,
         DatabaseBeans.class,
         WebGatewayBeans.class,
         TwitchBeans.class,
         MetricBeans.class,
         EngineBeans.class,
         UtilBeans.class,
         FinanceBeans.class,
         FactoryBeans.class,
         DispatcherBeans.class,
         CommonCacheBeans.class,
         ComputerBeans.class,
         ChatConfigBeans.class,
         AWSBeans.class})
@Configuration
public class ApplicationContext {

    @Bean
    public LastOrderCache lastOrderCache() {
        return new LastOrderCache();
    }

}
