package data.factory;

import application.Config;
import com.cheddar.robinhood.data.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.computer.QuoteComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.*;
import stockstream.database.InstrumentStub;
import stockstream.database.Wallet;
import stockstream.database.WalletRegistry;
import utils.GameUtil;

import java.util.concurrent.ExecutionException;


public class ResponseFactory {

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private GameUtil gameUtil;

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private WalletComputer walletComputer;

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private QuoteComputer quoteComputer;

    public String constructResponse(final OrderResult orderResult, final int votesReceived) {
        final String commandString = String.valueOf(orderResult.getAction()).toLowerCase();
        String orderMessage = this.gameUtil.getGenericErrorMessage();

        switch (orderResult.getOrderStatus()) {
            case OK: {
                if (TradeAction.SKIP.toString().equals(orderResult.getAction())) {
                    orderMessage = String.format("The winning vote is %s, with %s votes. Skipping a round!",
                                                 commandString, votesReceived);
                } else {
                    orderMessage = String.format("Placed order to %s a share of %s after receiving %s votes.",
                                                 commandString, orderResult.getSymbol(), votesReceived);
                }
                break;
            }
            case BAD_LIMIT: {
                orderMessage = "Limit invalid! Relative to last trade price, orders must be within 1% afterhours otherwise 5% for buy orders and 3% for sell orders.";
                break;
            }
            case CANT_AFFORD: {
                orderMessage = String.format("Received %s votes to %s %s, but cannot afford it!",
                                             votesReceived, commandString, orderResult.getSymbol());
                break;
            }
            case NET_WORTH_TOO_LOW: {
                orderMessage = String.format("Trading with a net worth of less than %s is not permitted due to rules imposed by FINRA. " +
                                             "In order to continue trading please either donate some funds or petition the appropriate authorities to remove these rules.",
                                             Config.MIN_NET_WORTH);
                break;
            }
            case NO_SHARES: {
                orderMessage = String.format("Received %s votes to %s %s, but could not because there are no shares of %s to %s.",
                                             votesReceived, commandString, orderResult.getSymbol(), orderResult.getSymbol(), commandString);
                break;
            }
            case NOT_ENOUGH_VOTES: {
                orderMessage = String.format("Not enough votes cast to %s %s! For stocks greater than $300 at least 2 votes needed. " +
                                             "For greater than $1000, 3 votes needed and 5 votes needed for stocks greater than $2000.", commandString, orderResult.getSymbol());
                break;
            }
            case MARKET_CLOSED: {
                orderMessage = String.format("Received %s votes to %s %s, but unfortunately the market closed before the order could be placed!",
                                             votesReceived, commandString, orderResult.getSymbol());
                break;
            }
            case BROKER_EXCEPTION: {
                orderMessage = String.format("Received %s votes to %s %s, but ran into an snafu. See if you can make some sense of it: %s",
                                             votesReceived, commandString, orderResult.getSymbol(), this.gameUtil.getGenericErrorMessage());
                break;
            }
            case NOT_ENOUGH_BUYING_POWER: {
                orderMessage = String.format("Not enough voting power to %s %s. The sum of voting power of all voters must be greater than the price of the stock. " +
                                             "Use the #w command to check your voting power.", commandString, orderResult.getSymbol());
                break;
            }
            case UNKNOWN: {
                orderMessage = String.format("Not sure exactly what is going on anymore. %s {} %s () %s [] %s @" +
                                             this.gameUtil.getGenericErrorMessage(), commandString, orderResult.toString(), votesReceived);
                break;
            }
        }

        return orderMessage;
    }

    public String constructPreResponse(final String action, final String symbol, final OrderStatus orderStatus) throws ExecutionException {
        final Quote quote = brokerCache.getQuoteForSymbol(symbol);

        String orderMessage = this.gameUtil.getGenericErrorMessage();

        switch (orderStatus) {
            case BAD_LIMIT: {
                orderMessage = "Limit invalid! Buy and Sell order limits cannot exceed 1% of most recent quote.";
                break;
            }
            case CANT_AFFORD: {
                orderMessage = String.format("Cannot place order for %s since it costs $%.2f and there is only $%.2f available.",
                                             symbol, quoteComputer.computeMostRecentPrice(quote), brokerCache.getAccountBalance().getUnallocated_margin_cash());
                break;
            }
            case EXCESS_CASH_AVAILABLE: {
                orderMessage = String.format("Cannot %s %s because you don't own it and there is enough cash for you to buy whatever you want.",
                                             action, symbol);
                break;
            }
            case NO_SHARES: {
                orderMessage = String.format("There are no shares of %s to %s.", symbol, action);
                break;
            }
        }

        return orderMessage;
    }

    public String constructWalletCommandResponse(final String player, final WalletOrderResult walletOrderResult) throws ExecutionException {
        final WalletCommand walletCommand = walletOrderResult.getWalletCommand();
        final int completedOrders = walletOrderResult.getExecutedShares();

        final Wallet wallet = walletRegistry.getWallet(player);
        final String action = walletCommand.getAction().toString();
        final double walletBalance = walletComputer.computeSpendingBalance(wallet);
        final String symbol = walletCommand.getParameter();
        final int quantity = walletCommand.getQuantity();
        final InstrumentStub instrument = instrumentCache.getSymbolToInstrument().get(symbol);

        Quote quote = null;
        try {
            quote = brokerCache.getQuoteForSymbol(symbol);
        } catch (final Exception ex) {
        }

        String orderMessage = "";

        if (completedOrders < walletCommand.getQuantity()) {
            orderMessage = String.format("Only %s of %s orders placed. ", completedOrders, quantity);
        }

        switch (walletOrderResult.getOrderStatus()) {
            case OK: {
                orderMessage = String.format("Order successfully placed! %s %s %s $%.2f", action, quantity, symbol, walletCommand.getLimit());
                break;
            }
            case BAD_LIMIT: {
                orderMessage = "Limit invalid! Buy and Sell order must be collared by no more than 1% of most recent quote and cannot be offset by more than 10%.";
                break;
            }
            case CANT_AFFORD: {
                orderMessage = String.format("Cannot place order for %s since it costs $%.2f and there is only $%.2f available.",
                                             symbol, quoteComputer.computeMostRecentPrice(quote), brokerCache.getAccountBalance().getUnallocated_margin_cash());
                break;
            }
            case BALANCE_TOO_LOW: {
                orderMessage = String.format("Cannot %s %s %s $%.2f because your wallet only has $%.2f.",
                                             action, quantity, symbol, walletCommand.getLimit(), walletBalance);
                break;
            }
            case BAD_TICK_SIZE: {
                orderMessage = String.format("The SEC mandates that limit orders for %s must be rounded to %.2f. Learn More: https://goo.gl/T6gcU6",
                                             symbol, instrument.getMin_tick_size());
                break;
            }
            case NET_WORTH_TOO_LOW: {
                orderMessage = String.format("Daytrading with a net worth of less than %s is not permitted due to rules imposed by FINRA. " +
                                             "In order to continue trading please either donate some funds or petition the appropriate authorities to remove these rules.",
                                             Config.MIN_NET_WORTH);
                break;
            }
            case NO_SHARES: {
                orderMessage = String.format("There not enough shares of %s to %s.", symbol, action);
                break;
            }
            case BROKER_EXCEPTION: {
                orderMessage = String.format("Cannot %s %s, bad response from Robinhood.", action, symbol);
                break;
            }
            case INVALID_COMMAND: {
                orderMessage = String.format("Cannot %s %s %s - invalid command! Please see the !tutorial for help.", action, symbol, walletCommand.getLimit());
                break;
            }
            case UNKNOWN: {
                orderMessage = String.format("Cannot %s %s %s %s, an unknown error occurred.", action, quantity, symbol, walletCommand.getLimit());
                break;
            }
        }

        return orderMessage;
    }
}
