package logic.voting;

import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import data.RoundResult;
import data.factory.CommandFactory;
import data.factory.ResponseFactory;
import logic.game.GameEngine;
import lombok.extern.slf4j.Slf4j;
import network.gateway.aws.RoundPublisher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.computer.OrderComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.*;
import stockstream.database.Wallet;
import stockstream.database.WalletRegistry;
import stockstream.logic.PubSub;
import stockstream.logic.elections.Election;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class TradingElection extends Election<TradeCommand> {

    @Autowired
    private RoundPublisher roundPublisher;

    @Autowired
    private CommandFactory commandFactory;

    @Autowired
    private OrderComputer orderComputer;

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    private PubSub pubSub;

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletComputer walletComputer;

    @Autowired
    private GameEngine gameEngine;

    public TradingElection() {
        super("!trading", TradeCommand.class, 0);
    }

    @PostConstruct
    public void init() {
        this.withMessageParser(m -> commandFactory.constructTradeCommand(m))
            .withVotePreProcessor(this::tryPreProcess)
            .withOutcome(this::onElection);
    }

    private Optional<String> tryPreProcess(final TradeCommand tradeCommand, final Voter voter) {
        try {
            return preProcessTradeCommand(tradeCommand, voter);
        } catch (final ExecutionException | RobinhoodException e) {
            log.warn("{} -> {}", tradeCommand, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @VisibleForTesting
    protected Optional<String> preProcessTradeCommand(final TradeCommand tradeCommand, final Voter voter) throws ExecutionException, RobinhoodException {
        if (tradeCommand.getAction().equals(TradeAction.SKIP)) {
            return Optional.empty();
        }

        final OrderStatus orderStatus = orderComputer.preProcessTradeCommand(tradeCommand, ImmutableSet.of(voter));
        if (!orderStatus.equals(OrderStatus.OK)) {
            final String response = responseFactory.constructPreResponse(tradeCommand.getAction().toString(), tradeCommand.getParameter(), orderStatus);
            if (!StringUtils.isEmpty(response)) {
                return Optional.of(response);
            }
        }
        return Optional.empty();
    }

    @VisibleForTesting
    protected Void onElection(final TradeCommand tradeCommand) {
        final SortedMap<TradeCommand, Set<Voter>> sortedCandidateToVoters = this.getSortedCandidateToVoters();
        final Map<Voter, TradeCommand> voterToCandidate = this.getVoterToCandidate();
        final Map<String, Voter> playerIdToVoter = voterToCandidate.keySet().stream().collect(Collectors.toMap(Voter::getPlayerId, v -> v));

        final OrderResult orderResult = gameEngine.executeBestCommand(sortedCandidateToVoters);

        final Set<String> playerIdsOverLimit = findPlayersOverLimit(sortedCandidateToVoters);

        playerIdsOverLimit.forEach(player -> voterToCandidate.remove(playerIdToVoter.get(player)));

        final RoundResult roundResult = new RoundResult(voterToCandidate, sortedCandidateToVoters, orderResult);

        pubSub.publishClassType(OrderResult.class, orderResult);

        roundPublisher.publishRoundResult(roundResult);

        return null;
    }

    @VisibleForTesting
    private Set<String> findPlayersOverLimit(final SortedMap<TradeCommand, Set<Voter>> candidateToVoters) {
        final Set<String> playerIdsOverLimit = new HashSet<>();

        if (candidateToVoters.size() <= 0) {
            return playerIdsOverLimit;
        }

        final Map.Entry<TradeCommand, Set<Voter>> winningTradeEntry = candidateToVoters.entrySet().iterator().next();
        final TradeCommand winningTrade = winningTradeEntry.getKey();

        if (winningTrade.getAction().equals(TradeAction.BUY)) {
            final Set<String> voters = winningTradeEntry.getValue().stream().map(Voter::getPlayerId).collect(Collectors.toSet());
            final List<Wallet> playerWallets = walletRegistry.getWallets(voters);
            playerWallets.forEach(wallet -> {
                final double votingPower = walletComputer.computeBuyingPower(wallet);
                if (votingPower <= 0) {
                    playerIdsOverLimit.add(wallet.getPlatform_username());
                }
            });
        }

        return playerIdsOverLimit;
    }

}
