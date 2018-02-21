package data;

import com.cheddar.robinhood.data.Execution;
import com.cheddar.robinhood.data.Quote;
import com.google.common.collect.ImmutableList;
import stockstream.data.Voter;
import stockstream.database.RobinhoodOrder;
import stockstream.database.WalletOrder;

public class TestDataUtils {

    public static Quote createQuote(final String symbol, final double lastPrice) {
        return new Quote(symbol, 2.50, 2.45, 10, 10, 2.49, 0, lastPrice, lastPrice, "http://instrument");
    }

    public static WalletOrder createWalletOrder(final String player, final String side, final String symbol, final String id, final String sellId, final String created_at) {
        final WalletOrder walletOrder = new WalletOrder(0, id, created_at, side, "1", player, sellId, symbol);
        return walletOrder;
    }

    public static RobinhoodOrder createRobinhoodOrder(final String side, final String symbol, final String id, final String created_at) {
        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder(id, "filled", created_at, "100.55", "101.25", side, "1", symbol, "");
        robinhoodOrder.setExecutions(ImmutableList.of(new Execution("1", created_at)));
        return robinhoodOrder;
    }

    public static Voter createVoter(final String player) {
        return createVoter(player, false);
    }

    public static Voter createVoter(final String player, final boolean subscriber) {
        return new Voter(player, "twitch", "#stockstream", subscriber);
    }

}
