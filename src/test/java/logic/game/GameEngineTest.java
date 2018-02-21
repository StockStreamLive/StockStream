package logic.game;


import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.MarginBalances;
import com.cheddar.robinhood.data.Order;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import data.TestDataUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.computer.AssetComputer;
import stockstream.computer.OrderComputer;
import stockstream.computer.TimeComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.*;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;
import stockstream.logic.elections.VoteComparator;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GameEngineTest {

    @Mock
    private RobinhoodAPI broker;

    @Mock
    private BrokerCache brokerCache;

    @Mock
    private TimeComputer timeComputer;

    @Mock
    private AssetComputer assetComputer;

    @Mock
    private OrderComputer orderComputer;

    @Mock
    private Scheduler scheduler;

    @Mock
    private WalletComputer walletComputer;

    @Mock
    private InstrumentCache instrumentCache;

    @InjectMocks
    private GameEngine gameEngine;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTick_zeroVoters_expectNotEnoughVotes() {
        when(timeComputer.isMarketOpenNow()).thenReturn(true);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NOT_ENOUGH_VOTES));
    }

    @Test
    public void testTick_someCommandMarketClosed_expectNoAction() {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        when(timeComputer.isMarketOpenNow()).thenReturn(false);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of());

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);
        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.MARKET_CLOSED));

        verify(timeComputer, times(1)).isMarketOpenNow();
    }

    @Test
    public void testTick_someCommandNetWorthTooLow_expectNoAction() {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(100d);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1"),
                                                             TestDataUtils.createVoter("twitch:player2")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);
        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NET_WORTH_TOO_LOW));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_someBuyCommandHappyCase_expectBuyCommand() throws RobinhoodException, ExecutionException {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        final Quote quote = new Quote();
        quote.setInstrument("instrument");
        quote.setLast_trade_price(150);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setMin_tick_size(0);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(25000f, 2000, 123, 4321.0));
        when(orderComputer.calculateBuyOrderCeiling(any())).thenReturn(1d);
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(orderComputer.constructLimitOrderString(anyDouble(), anyFloat())).thenReturn("0");
        when(instrumentCache.getUrlToInstrument()).thenReturn(ImmutableMap.of("instrument", instrument));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(151d);
        doNothing().when(scheduler).notifyEvent(anyObject());

        final Order okOrder = new Order("id", "filled", "0", "1", "1", "", "buy", "", "1", ImmutableList.of());
        when(broker.buyShares("AAPL", 1, 1.2)).thenReturn(okOrder);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1"),
                                                             TestDataUtils.createVoter("twitch:player2")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.OK));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_oneVote500DollarStock_expectNotEnoughVotes() throws RobinhoodException, ExecutionException {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        final Quote quote = new Quote();
        quote.setInstrument("instrument");
        quote.setLast_trade_price(501);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setMin_tick_size(0);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(25000f, 2000, 123, 4321.0));
        when(orderComputer.calculateBuyOrderCeiling(any())).thenReturn(1d);
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(orderComputer.constructLimitOrderString(anyDouble(), anyFloat())).thenReturn("0");
        when(instrumentCache.getUrlToInstrument()).thenReturn(ImmutableMap.of("instrument", instrument));
        doNothing().when(scheduler).notifyEvent(anyObject());

        final Order okOrder = new Order("id", "filled", "0", "1", "1", "", "buy", "", "1", ImmutableList.of());
        when(broker.buyShares("AAPL", 1, 1.2)).thenReturn(okOrder);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NOT_ENOUGH_VOTES));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_oneVote1000DollarStock_expectNotEnoughVotes() throws RobinhoodException, ExecutionException {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        final Quote quote = new Quote();
        quote.setInstrument("instrument");
        quote.setLast_trade_price(1001);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setMin_tick_size(0);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(25000f, 2000, 123, 4321.0));
        when(orderComputer.calculateBuyOrderCeiling(any())).thenReturn(1d);
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(orderComputer.constructLimitOrderString(anyDouble(), anyFloat())).thenReturn("0");
        when(instrumentCache.getUrlToInstrument()).thenReturn(ImmutableMap.of("instrument", instrument));
        doNothing().when(scheduler).notifyEvent(anyObject());

        final Order okOrder = new Order("id", "filled", "0", "1", "1", "", "buy", "", "1", ImmutableList.of());
        when(broker.buyShares("AAPL", 1, 1.2)).thenReturn(okOrder);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1"),
                                                             TestDataUtils.createVoter("twitch:player2")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NOT_ENOUGH_VOTES));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_twoVotesNotEnoughBuyingPower_expectNotEnoughBuyingPower() throws RobinhoodException, ExecutionException {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        final Quote quote = new Quote();
        quote.setInstrument("instrument");
        quote.setLast_trade_price(550);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setMin_tick_size(0);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(25000f, 2000, 123, 4321.0));
        when(orderComputer.calculateBuyOrderCeiling(any())).thenReturn(1d);
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(orderComputer.constructLimitOrderString(anyDouble(), anyFloat())).thenReturn("0");
        when(instrumentCache.getUrlToInstrument()).thenReturn(ImmutableMap.of("instrument", instrument));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(151d);
        doNothing().when(scheduler).notifyEvent(anyObject());

        final Order okOrder = new Order("id", "filled", "0", "1", "1", "", "buy", "", "1", ImmutableList.of());
        when(broker.buyShares("AAPL", 1, 1.2)).thenReturn(okOrder);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1"),
                                                             TestDataUtils.createVoter("twitch:player2")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NOT_ENOUGH_BUYING_POWER));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_twoVoteZeroBuyingPower_expectNotEnoughBuyingPower() throws RobinhoodException, ExecutionException {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.BUY, "AAPL");

        final Quote quote = new Quote();
        quote.setInstrument("instrument");
        quote.setLast_trade_price(550);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setMin_tick_size(0);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(25000f, 2000, 123, 4321.0));
        when(orderComputer.calculateBuyOrderCeiling(any())).thenReturn(1d);
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(orderComputer.constructLimitOrderString(anyDouble(), anyFloat())).thenReturn("0");
        when(instrumentCache.getUrlToInstrument()).thenReturn(ImmutableMap.of("instrument", instrument));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(151d);
        doNothing().when(scheduler).notifyEvent(anyObject());

        final Order okOrder = new Order("id", "filled", "0", "1", "1", "", "buy", "", "1", ImmutableList.of());
        when(broker.buyShares("AAPL", 1, 1.2)).thenReturn(okOrder);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1"),
                                                             TestDataUtils.createVoter("twitch:player2")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.NOT_ENOUGH_BUYING_POWER));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }

    @Test
    public void testTick_twoVoteZeroBuyingPowerSellCommand_expectOK() throws RobinhoodException, ExecutionException {
        final TradeCommand bestCommand = new TradeCommand(TradeAction.SELL, "AAPL");

        final Quote quote = new Quote();
        quote.setInstrument("instrument");
        quote.setLast_trade_price(550);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setMin_tick_size(0);

        when(timeComputer.isMarketOpenNow()).thenReturn(true);
        when(brokerCache.getAccountNetWorth()).thenReturn(50000d);
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(25000f, 2000, 123, 4321.0));
        when(orderComputer.calculateBuyOrderCeiling(any())).thenReturn(1d);
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(orderComputer.constructLimitOrderString(anyDouble(), anyFloat())).thenReturn("0");
        when(instrumentCache.getUrlToInstrument()).thenReturn(ImmutableMap.of("instrument", instrument));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(151d);
        doNothing().when(scheduler).notifyEvent(anyObject());

        final Order okOrder = new Order("id", "filled", "0", "1", "1", "", "buy", "", "1", ImmutableList.of());
        when(broker.buyShares("AAPL", 1, 1.2)).thenReturn(okOrder);

        final SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands = new TreeMap<>(new VoteComparator<>(ImmutableMap.of()));
        rankedTradeCommands.put(bestCommand, ImmutableSet.of(TestDataUtils.createVoter("twitch:player1"),
                                                             TestDataUtils.createVoter("twitch:player2")));

        final OrderResult orderResult = gameEngine.executeBestCommand(rankedTradeCommands);

        assertTrue(orderResult.getOrderStatus().equals(OrderStatus.OK));

        verify(timeComputer, times(1)).isMarketOpenNow();
        verify(brokerCache, times(1)).getAccountNetWorth();
    }
}
