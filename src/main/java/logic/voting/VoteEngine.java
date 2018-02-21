package logic.voting;

import data.Vote;
import logic.game.GameClock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.gateway.aws.MetricPublisher;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;
import stockstream.cache.BrokerCache;
import stockstream.data.Voter;
import stockstream.database.ElectionRegistry;
import stockstream.logic.Scheduler;
import stockstream.logic.elections.Election;
import stockstream.twitch.TwitchAPI;
import stockstream.twitch.TwitchChat;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class VoteEngine extends ListenerAdapter {

    @Autowired
    private GameClock gameClock;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private MetricPublisher metricPublisher;

    @Autowired
    private TwitchAPI twitchAPI;

    @Autowired
    private ElectionRegistry electionRegistry;

    @Autowired
    private TwitchChat twitchChat;

    @Autowired
    private TradingElection tradingElection;

    @Autowired
    private WalletElection walletElection;

    @Autowired
    private SpeedElection speedElection;

    @Autowired
    private BrokerCache brokerCache;

    private final int MAX_EXECUTOR_JOBS = 5;
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_EXECUTOR_JOBS, f -> new Thread(f, "votePoll"));

    private final List<Election<?>> elections = new ArrayList<>();

    private final Set<String> activePlayers = ConcurrentHashMap.newKeySet();

    @Getter
    private Queue<Vote> voteQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        elections.add(tradingElection);
        elections.add(speedElection);
        elections.add(walletElection);
        scheduler.scheduleJob(this::multiThreadedPoll, 5000, 200, TimeUnit.MILLISECONDS);
    }

    public synchronized void executeElections() {
        elections.forEach(Election::executeOutcome);
        publishMetrics();
        activePlayers.clear();
    }

    public synchronized void updateElectionExpirations() {
        elections.forEach(election -> election.setExpirationDate(gameClock.getNextGameEvent().getNextEvent()));
        electionRegistry.archiveElections(elections);
    }

    private void publishMetrics() {
        final Set<Voter> voterSet = new HashSet<>();
        elections.forEach(election -> election.getCandidateToVoters().values().forEach(voterSet::addAll));

        final int totalVoters = voterSet.size();
        final int activePlayerCount = activePlayers.size();
        final int totalAudience = twitchAPI.getViewers().size();
        final double spendableCash = brokerCache.getAccountBalance().getUnallocated_margin_cash();

        metricPublisher.publishMetric("TotalVoters", totalVoters);
        metricPublisher.publishMetric("ActivePlayers", activePlayerCount);
        metricPublisher.publishMetric("TotalViewers", totalAudience);
        metricPublisher.publishMetric("SpendableCash", spendableCash);
    }

    @Override
    public void onMessage(final MessageEvent event) {
        if (event.getUser() == null) {
            return;
        }

        final String fromChannel = event.getChannelSource();
        final String nick = event.getUser().getNick();

        activePlayers.add(nick);

        final String eventMessage = event.getMessage().toLowerCase();

        if (StringUtils.isEmpty(eventMessage)) {
            return;
        }

        final boolean isSubscriber = "1".equals(event.getTags().getOrDefault("subscriber", "0"));

        final String vote = eventMessage.toLowerCase();
        final Voter voter = new Voter(nick, "twitch", fromChannel, isSubscriber);

        voteQueue.add(new Vote(voter, vote, fromChannel, new Date().getTime()));
    }

    public synchronized void enqueueVote(final Vote vote) {
        voteQueue.add(vote);
    }

    public synchronized void multiThreadedPoll() {
        final long now = new Date().getTime();
        final Set<Election<?>> unexpiredElections = elections.stream().filter(election -> now < election.getExpirationDate()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(unexpiredElections)) {
            log.info("{} uncounted votes.", voteQueue.size());
            return;
        }

        final int votesToProcess = voteQueue.size();

        if (votesToProcess <= 0) {
            return;
        }

        final int jobsToSubmit = Math.min(votesToProcess, MAX_EXECUTOR_JOBS);
        final int votesPerJob = (int) Math.ceil(votesToProcess / (float) jobsToSubmit);

        final List<Future<?>> executorFutures = new ArrayList<>(votesToProcess);

        for (int i = 0; i < jobsToSubmit; ++i) {
            executorFutures.add(executorService.submit(() -> processVotes(votesPerJob)));
        }

        try {
            for (final Future<?> executorFuture : executorFutures) {
                executorFuture.get();
            }
        } catch (final CancellationException | InterruptedException | ExecutionException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void processVotes(final int votesToProcess) {
        for (int x = 0; x < votesToProcess; ++x) {
            final Vote vote = voteQueue.poll();
            if (vote == null) {
                continue;
            }

            final boolean requeueVote = processVote(vote);

            if (requeueVote) {
                voteQueue.add(vote);
            }
        }
    }

    private boolean processVote(final Vote vote) {
        boolean requeueVote = false;

        for (final Election<?> election : elections) {
            if (vote.getTimestamp() < election.getExpirationDate()) {
                final Optional<String> response = election.receiveVote(vote.getVote(), vote.getVoter());
                response.ifPresent(s -> twitchChat.enqueueMessage(vote.getFromChannel(), String.format("@%s %s", vote.getVoter().getUsername(), s)));
            } else {
                requeueVote = true;
            }
        }

        return requeueVote;
    }

}
