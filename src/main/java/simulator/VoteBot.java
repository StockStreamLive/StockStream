package simulator;

import application.Config;
import application.Stage;
import com.google.common.collect.ImmutableSet;
import data.Vote;
import logic.voting.VoteEngine;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.InstrumentCache;
import stockstream.data.Voter;
import stockstream.logic.Scheduler;
import stockstream.util.RandomUtil;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class VoteBot {

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private VoteEngine voteEngine;

    @Autowired
    private Scheduler scheduler;

    private static final int BOT_COUNT = 5000;
    private static final int VOTES_PER_SECOND = 1;

    private static final Set<String> SYMBOLS = ImmutableSet.of("AAPL", "FB", "SGYP", "RAD", "RIOT", "APTO", "BUR", "ENSV");
    private static final Set<String> ACTIONS = ImmutableSet.of("buy", "sell");

    private final List<Voter> bots = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!Config.stage.equals(Stage.TEST)) {
            return;
        }

        for (int i = 0; i < BOT_COUNT; ++i) {
            bots.add(new Voter(String.format("bot_%s", i), "simulator", "stockstream", false));
        }

        for (int i = 0; i < VOTES_PER_SECOND; ++i) {
            scheduler.scheduleJob(this::castVotes, 1, 1, TimeUnit.SECONDS);
        }
    }

    private void castVotes() {
        final Voter randomBot = RandomUtil.randomChoice(bots).get();
        final String randomAction = RandomUtil.randomChoice(ACTIONS).get();
        final String randomStock = RandomUtil.randomChoice(SYMBOLS).get();

        final String voteString = String.format("!%s %s", randomAction, randomStock);

        voteEngine.enqueueVote(new Vote(randomBot, voteString, "#stockstream", new Date().getTime()));
    }

}
