package logic.game;

import application.Config;
import application.Stage;
import com.cheddar.robinhood.data.MarketState;
import com.google.common.annotations.VisibleForTesting;
import logic.voting.VoteEngine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.base.BaseDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.BrokerCache;
import stockstream.computer.TimeComputer;
import stockstream.database.GameStateRegistry;
import stockstream.database.GameStateStub;
import stockstream.logic.Scheduler;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GameClock {

    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeComputer timeComputer;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private GameStateRegistry gameStateRegistry;

    @Autowired
    private VoteEngine voteEngine;

    @Getter
    private int roundLengthSeconds = Config.stage == Stage.PROD ? 150 : 30;

    private static final int STEP_SECONDS = 30;
    private static final int MIN_LENGTH_SECONDS = 60;
    private static final int MAX_LENGTH_SECONDS = 600;

    @Getter
    private GameEvent nextGameEvent = new GameEvent(0, GameEvent.Type.MARKET_OPEN);

    public void speedUp() {
        roundLengthSeconds -= STEP_SECONDS;
        if (roundLengthSeconds < MIN_LENGTH_SECONDS) {
            roundLengthSeconds = MIN_LENGTH_SECONDS;
        }
    }

    public void slowDown() {
        roundLengthSeconds += STEP_SECONDS;
        if (roundLengthSeconds > MAX_LENGTH_SECONDS) {
            roundLengthSeconds = MAX_LENGTH_SECONDS;
        }
    }

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::doApplicationTick, 1000, 200, TimeUnit.MILLISECONDS);
    }

    private void doApplicationTick() {
        final DateTime now = new DateTime();

        if (now.isAfter(nextGameEvent.getNextEvent())) {
            log.info("Now {} is after next event {}.", now, nextGameEvent);

            if (nextGameEvent.getNextEvent() > 0) {
                voteEngine.executeElections();
            }

            setNextGameEvent(findNextEvent(now));
        }
    }

    private void setNextGameEvent(final GameEvent gameEvent) {
        nextGameEvent = gameEvent;
        gameStateRegistry.saveGameStateStub(new GameStateStub(nextGameEvent.getNextEvent(), nextGameEvent.getEventType().name()));
        voteEngine.updateElectionExpirations();
    }

    @VisibleForTesting
    protected GameEvent findNextEvent(final DateTime now) {
        final MarketState marketStateToday = this.brokerCache.getMarketState(now);

        final long nextGameTick = System.currentTimeMillis() + (getRoundLengthSeconds() * 1000);
        if (marketStateToday.isOpenNow()) {
            return new GameEvent(nextGameTick, GameEvent.Type.GAME_TICK);
        }

        if (marketStateToday.isOpenThisDay() &&
            now.isBefore(marketStateToday.getExtendedOpenTime().get()) &&
            now.isBefore(marketStateToday.getExtendedCloseTime().get())) {
            final long openTimeToday = marketStateToday.getExtendedOpenTime().map(BaseDateTime::getMillis).orElse(nextGameTick);
            return new GameEvent(openTimeToday, GameEvent.Type.MARKET_OPEN);
        }

        final MarketState nextBusinessDay = this.timeComputer.findNextBusinessDay(now);
        final long nextBusinessDayOpenTime = nextBusinessDay.getExtendedOpenTime().map(BaseDateTime::getMillis).orElse(nextGameTick);
        return new GameEvent(nextBusinessDayOpenTime, GameEvent.Type.MARKET_OPEN);
    }

}
