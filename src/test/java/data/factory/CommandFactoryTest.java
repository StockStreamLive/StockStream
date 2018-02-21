package data.factory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.computer.AssetComputer;
import stockstream.data.TradeAction;
import stockstream.data.TradeCommand;
import stockstream.data.WalletAction;
import stockstream.data.WalletCommand;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class CommandFactoryTest {

    @Mock
    private AssetComputer assetComputer;

    @InjectMocks
    private CommandFactory commandFactory;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConstructTradeCommand_invalidCommand_expectEmpty() {
        assertEquals(Optional.empty(), commandFactory.constructTradeCommand("buy aapl"));
        assertEquals(Optional.empty(), commandFactory.constructTradeCommand("#buy aapl"));
        assertEquals(Optional.empty(), commandFactory.constructTradeCommand("!buy aapslkfjadskmnkdmakldsceimcal;ismdaildixml"));
        assertEquals(Optional.empty(), commandFactory.constructTradeCommand("!buy aapl 1"));
    }

    @Test
    public void testConstructTradeCommand_skipHold_expectSkip() {
        final TradeCommand skipCommand = new TradeCommand(TradeAction.SKIP, null);

        assertEquals(Optional.of(skipCommand), commandFactory.constructTradeCommand("!skip"));
        assertEquals(Optional.of(skipCommand), commandFactory.constructTradeCommand("!hodl"));
    }

    @Test
    public void testConstructTradeCommand_invalidSymbol_expectEmpty() {
        when(assetComputer.isSymbol(any())).thenReturn(false);

        assertEquals(Optional.empty(), commandFactory.constructTradeCommand("!buy oopl"));
    }

    @Test
    public void testConstructTradeCommand_validSymbolInvalidAction_expectEmpty() {
        when(assetComputer.isSymbol(any())).thenReturn(true);

        assertEquals(Optional.empty(), commandFactory.constructTradeCommand("!foo aapl"));
    }

    @Test
    public void testConstructTradeCommand_validSymbolValidBuyAction_expectValidResponse() {
        when(assetComputer.isSymbol(any())).thenReturn(true);

        Optional<TradeCommand> tradeCommandOptional = commandFactory.constructTradeCommand("!buy aapl");

        assertEquals(TradeAction.BUY, tradeCommandOptional.get().getAction());
        assertEquals("AAPL", tradeCommandOptional.get().getParameter());
    }

    @Test
    public void testConstructTradeCommand_validSymbolValidSellAction_expectValidResponse() {
        when(assetComputer.isSymbol(any())).thenReturn(true);

        Optional<TradeCommand> tradeCommandOptional = commandFactory.constructTradeCommand("!sell aapl");

        assertEquals(TradeAction.SELL, tradeCommandOptional.get().getAction());
        assertEquals("AAPL", tradeCommandOptional.get().getParameter());
    }

    @Test
    public void testConstructWalletCommand_invalidCommand_expectEmpty() {
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("#buy aapl"));
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("buy aapl"));
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("!buy aapl"));
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("!buy aapslkfjadskmnkdmakldsceimcal;ismdaildixml"));
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("!buy aapl 1"));
    }

    @Test
    public void testConstructWalletCommand_invalidAction_expectEmpty() {
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("#by aapl 1.75"));
        assertEquals(Optional.empty(), commandFactory.constructWalletCommand("#selll aapl"));
    }

    @Test
    public void testConstructWalletCommand_validParams_expectValidResponse() {
        final WalletCommand sendCommand = new WalletCommand(WalletAction.SEND, 1, "player1", 4.20);
        final WalletCommand buyCommand = new WalletCommand(WalletAction.BUY, 1, "AMZN", 4.21);
        final WalletCommand sellCommand = new WalletCommand(WalletAction.SELL, 1, "GOOG", 4.22);

        when(assetComputer.isSymbol(any())).thenReturn(true);

        assertEquals(Optional.of(sendCommand).get(), commandFactory.constructWalletCommand("#send player1 4.20").get());
        assertEquals(Optional.of(buyCommand).get(), commandFactory.constructWalletCommand("#buy 1 AMZN 4.21").get());
        assertEquals(Optional.of(sellCommand).get(), commandFactory.constructWalletCommand("#sell 2 GOOG 4.22").get());
    }

}
