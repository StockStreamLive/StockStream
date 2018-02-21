package data;


import org.junit.Test;
import stockstream.data.TradeAction;
import stockstream.data.TradeCommand;

import static org.junit.Assert.*;

public class TradeCommandTest {

    @Test
    public void testEquals_sameCommand_expectEqual() {
        final TradeCommand tradeCommand1 = new TradeCommand(TradeAction.BUY, "AAPL");
        final TradeCommand tradeCommand2 = new TradeCommand(TradeAction.BUY, "AAPL");

        assertTrue(tradeCommand1.equals(tradeCommand2));
        assertEquals(tradeCommand1.hashCode(), tradeCommand2.hashCode());
    }

    @Test
    public void testEquals_sameActionDiffParam_expectNotEqual() {
        final TradeCommand tradeCommand1 = new TradeCommand(TradeAction.BUY, "AAPL");
        final TradeCommand tradeCommand2 = new TradeCommand(TradeAction.BUY, "GOOGL");

        assertFalse(tradeCommand1.equals(tradeCommand2));
        assertNotEquals(tradeCommand1.hashCode(), tradeCommand2.hashCode());
    }

    @Test
    public void testEquals_sameParamDiffAction_expectNotEqual() {
        final TradeCommand tradeCommand1 = new TradeCommand(TradeAction.BUY, "AAPL");
        final TradeCommand tradeCommand2 = new TradeCommand(TradeAction.SELL, "AAPL");

        assertFalse(tradeCommand1.equals(tradeCommand2));
        assertNotEquals(tradeCommand1.hashCode(), tradeCommand2.hashCode());
    }

}
