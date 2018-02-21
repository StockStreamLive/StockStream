package logic.game;

import application.Config;
import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.Order;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import data.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.computer.AssetComputer;
import stockstream.computer.OrderComputer;
import stockstream.computer.TimeComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.*;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class GameEngine {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private RobinhoodAPI broker;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private TimeComputer timeComputer;

    @Autowired
    private AssetComputer assetComputer;

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private OrderComputer orderComputer;

    @Autowired
    private WalletComputer walletComputer;

    public OrderResult executeBestCommand(final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommandsToPlayers) {

        if (CollectionUtils.isEmpty(rankedTradeCommandsToPlayers.keySet())) {
            return new OrderResult(null, "", OrderStatus.NOT_ENOUGH_VOTES, null);
        }

        final Map.Entry<TradeCommand, Set<Voter>> bestCommandEntry = rankedTradeCommandsToPlayers.entrySet().iterator().next();
        final TradeCommand bestTradeCommand = bestCommandEntry.getKey();
        final Set<String> voters = bestCommandEntry.getValue().stream().map(Voter::getPlayerId).collect(Collectors.toSet());
        final String symbol = bestTradeCommand.getParameter();

        if (!this.timeComputer.isMarketOpenNow()) {
            return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.MARKET_CLOSED, null);
        }

        log.info("Best tradeCommand: {}", bestTradeCommand);

        OrderStatus orderStatus = OrderStatus.UNKNOWN;

        final double netWorth = this.brokerCache.getAccountNetWorth();
        log.info("Got an account net worth of {}.", netWorth);
        if (netWorth <= Config.MIN_NET_WORTH) {
            log.warn("Got an account net worth of {} so not executing tradeCommand {}.", netWorth, bestTradeCommand);
            return new OrderResult(null, "", OrderStatus.NET_WORTH_TOO_LOW, null);
        }

        if (TradeAction.SKIP.equals(bestTradeCommand.getAction())) {
            return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.OK, null);
        }

        try {
            final Quote quote = brokerCache.getQuoteForSymbol(symbol);

            orderStatus = orderComputer.preProcessTradeCommand(bestTradeCommand, bestCommandEntry.getValue());

            if (orderStatus != OrderStatus.OK) {
                return new OrderResult(bestTradeCommand.getAction().toString(), symbol, orderStatus, null);
            }

            final double quoteMostRecentPrice = quote.getLast_trade_price();

            if (quoteMostRecentPrice > 300 && voters.size() < 2) {
                return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.NOT_ENOUGH_VOTES, null);
            } else if (quoteMostRecentPrice > 1000 && voters.size() < 3) {
                return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.NOT_ENOUGH_VOTES, null);
            } else if (quoteMostRecentPrice > 2000 && voters.size() < 5) {
                return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.NOT_ENOUGH_VOTES, null);
            }

            if (bestTradeCommand.getAction().equals(TradeAction.BUY)) {
                final double buyingPower = walletComputer.computeBuyingPower(voters);
                log.info("Got voting power {} and most recent price of {}", buyingPower, quoteMostRecentPrice);
                if (buyingPower<quoteMostRecentPrice){
                    return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.NOT_ENOUGH_BUYING_POWER, null);
                }
            }

            Order order = null;

            switch (bestTradeCommand.getAction()) {
                case BUY: {
                    order = processBuy(bestTradeCommand, quote);
                    break;
                } case SELL: {
                    order = processSell(bestTradeCommand, quote);
                    break;
                } case SKIP: {
                    orderStatus = OrderStatus.OK;
                    break;
                } default: {
                    log.warn("Invalid action type: {}", bestTradeCommand);
                    break;
                }
            }

            scheduler.notifyEvent(Event.ORDER_PLACED);

            log.info("Got status {} after executing tradeCommand {}", orderStatus, bestTradeCommand);

            return new OrderResult(bestTradeCommand.getAction().toString(), symbol, orderStatus, order);

        } catch (final RobinhoodException | ExecutionException e) {
            log.warn(e.getMessage(), e);
            return new OrderResult(bestTradeCommand.getAction().toString(), symbol, OrderStatus.BROKER_EXCEPTION, null);
        }
    }

    private Order processSell(final TradeCommand tradeCommand, final Quote quote) throws RobinhoodException {
        log.info("Executing SELL tradeCommand {} ", tradeCommand);

        final InstrumentStub instrument = instrumentCache.getUrlToInstrument().get(quote.getInstrument());

        final String symbolToSell = tradeCommand.getParameter();

        final double limit = orderComputer.calculateSellOrderFloor(quote);
        final String moddedLimit = orderComputer.constructLimitOrderString(limit, instrument.getMin_tick_size());

        return this.broker.sellShares(symbolToSell, 1, Double.valueOf(moddedLimit));
    }

    private Order processBuy(final TradeCommand tradeCommand, final Quote quote) throws RobinhoodException {
        log.info("Executing BUY tradeCommand {} ", tradeCommand);

        final InstrumentStub instrument = instrumentCache.getUrlToInstrument().get(quote.getInstrument());

        final double limit = orderComputer.calculateBuyOrderCeiling(quote);
        final String moddedLimit = orderComputer.constructLimitOrderString(limit, instrument.getMin_tick_size());

        return this.broker.buyShares(tradeCommand.getParameter(), 1, Double.valueOf(moddedLimit));
    }

}
