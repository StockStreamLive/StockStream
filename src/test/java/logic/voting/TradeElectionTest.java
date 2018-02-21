package logic.voting;

import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.ImmutableList;
import data.RoundResult;
import data.factory.CommandFactory;
import data.factory.ResponseFactory;
import logic.game.GameEngine;
import network.gateway.aws.RoundPublisher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import stockstream.computer.OrderComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.*;
import stockstream.database.ElectionRegistry;
import stockstream.database.Wallet;
import stockstream.database.WalletRegistry;
import stockstream.logic.PubSub;
import stockstream.spring.DatabaseBeans;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TradeElectionTest {

    @Mock
    private WalletRegistry walletRegistry;

    @Mock
    private GameEngine gameEngine;

    @Mock
    private PubSub pubSub;

    @Mock
    private OrderComputer orderComputer;

    @Mock
    private ResponseFactory responseFactory;

    @Mock
    private CommandFactory commandFactory;

    @Mock
    private WalletComputer walletComputer;

    @Mock
    private RoundPublisher roundPublisher;

    @InjectMocks
    private TradingElection tradingElection;

    private static ElectionRegistry electionRegistry;

    @BeforeClass
    public static void setupTestStatic() throws Exception {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

        context.register(DatabaseBeans.class);
        context.refresh();
        electionRegistry = context.getBean(ElectionRegistry.class);
    }

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        tradingElection.init();
        tradingElection.setElectionRegistry(electionRegistry);
        tradingElection.setExpirationDate(new Date().getTime());
    }

    @Test
    public void testPreProcessTradeCommand_skipCommand_expectEmptyResponse() throws ExecutionException, RobinhoodException {
        final Optional<String> preProcessResponse = tradingElection.preProcessTradeCommand(new TradeCommand(TradeAction.SKIP, ""),
                                                                                           new Voter("michrob", "twitch", "#stockstream", false));

        assertFalse(preProcessResponse.isPresent());
    }

    @Test
    public void testPreProcessTradeCommand_orderStatusNotOk_expectPassedResponse() throws ExecutionException, RobinhoodException {
        final String expectedResponse = "ACK";

        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.UNKNOWN);
        when(responseFactory.constructPreResponse(any(), any(), any())).thenReturn(expectedResponse);

        final Optional<String> preProcessResponse = tradingElection.preProcessTradeCommand(new TradeCommand(TradeAction.BUY, "ABC"),
                                                                                           new Voter("michrob", "twitch", "#stockstream", false));

        assertEquals(preProcessResponse.get(), expectedResponse);
    }

    @Test
    public void testPreProcessTradeCommand_orderStatusOk_expectEmptyResponse() throws ExecutionException, RobinhoodException {
        final String expectedResponse = "ACK";

        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(responseFactory.constructPreResponse(any(), any(), any())).thenReturn(expectedResponse);

        final Optional<String> preProcessResponse = tradingElection.preProcessTradeCommand(new TradeCommand(TradeAction.BUY, "ABC"),
                                                                                           new Voter("michrob", "twitch", "#stockstream", false));

        assertFalse(preProcessResponse.isPresent());
    }

    @Test
    public void testOnElection_threePlayersNoPlayersOverLimit_expectAllPresent() throws ExecutionException, RobinhoodException {

        final ArgumentCaptor<RoundResult> captor = ArgumentCaptor.forClass(RoundResult.class);

        when(walletRegistry.getWallets(any())).thenReturn(ImmutableList.of());
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(commandFactory.constructTradeCommand(any())).thenReturn(Optional.of(new TradeCommand(TradeAction.BUY, "ABC")));
        when(gameEngine.executeBestCommand(any())).thenReturn(new OrderResult("BUY", "ABC", OrderStatus.OK, null));

        tradingElection.receiveVote("!buy abc", new Voter("p1", "sim", "#sim", false));
        tradingElection.receiveVote("!buy abc", new Voter("p2", "sim", "#sim", false));
        tradingElection.receiveVote("!buy abc", new Voter("p3", "sim", "#sim", false));

        tradingElection.onElection(new TradeCommand(TradeAction.BUY, "ABC"));

        verify(pubSub, times(1)).publishClassType(any(), any());
        verify(roundPublisher, times(1)).publishRoundResult(captor.capture());

        final RoundResult roundResult = captor.getValue();

        assertEquals(3, roundResult.getPlayerToCommand().size());
    }

    @Test
    public void testOnElection_threePlayersOneOverLimit_expectTwoPresent() throws ExecutionException, RobinhoodException {

        final ArgumentCaptor<RoundResult> captor = ArgumentCaptor.forClass(RoundResult.class);

        when(walletRegistry.getWallets(any())).thenReturn(ImmutableList.of(new Wallet("sim:p1", 100, 100, 999999)));
        when(orderComputer.preProcessTradeCommand(any(), any())).thenReturn(OrderStatus.OK);
        when(commandFactory.constructTradeCommand(any())).thenReturn(Optional.of(new TradeCommand(TradeAction.BUY, "ABC")));
        when(gameEngine.executeBestCommand(any())).thenReturn(new OrderResult("BUY", "ABC", OrderStatus.OK, null));

        tradingElection.receiveVote("!buy abc", new Voter("p1", "sim", "#sim", false));
        tradingElection.receiveVote("!buy abc", new Voter("p2", "sim", "#sim", false));
        tradingElection.receiveVote("!buy abc", new Voter("p3", "sim", "#sim", false));

        tradingElection.onElection(new TradeCommand(TradeAction.BUY, "ABC"));

        verify(pubSub, times(1)).publishClassType(any(), any());
        verify(roundPublisher, times(1)).publishRoundResult(captor.capture());

        final RoundResult roundResult = captor.getValue();

        assertEquals(2, roundResult.getPlayerToCommand().size());
    }
}
