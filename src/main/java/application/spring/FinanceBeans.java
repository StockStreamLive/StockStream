package application.spring;

import application.Config;
import application.Stage;
import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.google.common.collect.ImmutableMap;
import simulator.VirtualFund;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricBeans.class})
@Configuration
public class FinanceBeans {

    @Bean
    public RobinhoodClient robinhood() {
        return new RobinhoodClient(Config.RH_UN, Config.RH_PW);
    }

    @Bean
    public VirtualFund virtualFund() {
        return new VirtualFund();
    }

    @Bean
    public ImmutableMap<Stage, RobinhoodAPI> brokers() {
        return ImmutableMap.of(Stage.TEST, virtualFund(),
                               Stage.LOCAL, virtualFund(),
                               Stage.GAMMA, virtualFund(),
                               Stage.PROD, robinhood());
    }

    @Bean
    public RobinhoodAPI broker() {
        return brokers().get(Config.stage);
    }

}
