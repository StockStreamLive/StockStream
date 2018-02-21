package application.spring;

import logic.game.GameClock;
import logic.game.GameEngine;
import logic.voting.SpeedElection;
import logic.voting.TradingElection;
import logic.voting.VoteEngine;
import logic.voting.WalletElection;
import logic.wallet.WalletEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({MetricBeans.class,
         FinanceBeans.class})
@Configuration
public class EngineBeans {

    @Bean
    public TradingElection tradingElection() {
        return new TradingElection();
    }

    @Bean
    public WalletElection walletElection() {
        return new WalletElection();
    }

    @Bean
    public SpeedElection speedElection() {
        return new SpeedElection();
    }

    @Bean
    public GameClock gameClock() {
        return new GameClock();
    }

    @Bean
    public VoteEngine voteEngine() {
        return new VoteEngine();
    }

    @Bean
    public GameEngine gameEngine() {
        return new GameEngine();
    }

    @Bean
    public WalletEngine walletEngine() {
        return new WalletEngine();
    }

}
