package application.spring;

import logic.wallet.WalletBot;
import network.gateway.twitch.Responder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import simulator.VoteBot;

@Import({EngineBeans.class,
         FactoryBeans.class})
@Configuration
public class DispatcherBeans {

    @Bean
    public Responder responder() {
        return new Responder();
    }

    @Bean
    public WalletBot walletBot() {
        return new WalletBot();
    }

    @Bean
    public VoteBot voteBot() {
        return new VoteBot();
    }

}
