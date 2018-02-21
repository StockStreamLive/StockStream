package data.factory;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.computer.AssetComputer;
import stockstream.data.TradeAction;
import stockstream.data.TradeCommand;
import stockstream.data.WalletAction;
import stockstream.data.WalletCommand;

import java.util.Optional;

public class CommandFactory {

    private static final int REJECT_LENGTH = 15;

    @Autowired
    private AssetComputer assetComputer;

    public Optional<TradeCommand> constructTradeCommand(final String input) {
        if (!input.startsWith("!")) {
            return Optional.empty();
        }

        if (input.length() > REJECT_LENGTH) {
            return Optional.empty();
        }

        final String message = input.trim().toUpperCase();

        if ("!skip".equalsIgnoreCase(message) || "!hodl".equalsIgnoreCase(message)) {
            return Optional.of(new TradeCommand(TradeAction.SKIP, StringUtils.EMPTY));
        }

        final String[] tokens = message.split("\\s+");

        if (tokens.length != 2) {
            return Optional.empty();
        }

        if (!this.assetComputer.isSymbol(tokens[1])) {
            return Optional.empty();
        }

        final String commandString = tokens[0].substring(1);

        TradeAction action = null;
        try {
            action = TradeAction.valueOf(commandString);
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }

        final String symbol = tokens[1];

        final TradeCommand newTradeCommand = new TradeCommand(action, symbol);
        return Optional.of(newTradeCommand);
    }

    public Optional<WalletCommand> constructWalletCommand(final String input) {
        if (!input.startsWith("#")) {
            return Optional.empty();
        }

        final String message = input.trim().toUpperCase();

        if (message.startsWith("#SEND")) {
            return constructSendCommand(message);
        }

        final String[] tokens = message.split("\\s+");

        if (tokens.length != 4) {
            return Optional.empty();
        }

        final String commandString = tokens[0].substring(1);


        WalletAction action = null;
        try {
            action = WalletAction.valueOf(commandString);
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }

        final String quantityString = tokens[1];
        final Integer quantity = Integer.valueOf(quantityString);

        final String symbol = tokens[2];
        if (!this.assetComputer.isSymbol(symbol)) {
            return Optional.empty();
        }

        final String limitString = tokens[3];
        final Double limitValue = Double.valueOf(limitString);

        final WalletCommand newWalletCommand = new WalletCommand(action, quantity, symbol, limitValue);
        return Optional.of(newWalletCommand);
    }

    private Optional<WalletCommand> constructSendCommand(final String message) {
        final String[] tokens = message.split("\\s+");

        if (tokens.length != 3) {
            return Optional.empty();
        }

        final String commandString = tokens[0].substring(1);

        WalletAction action = null;
        if ("send".equalsIgnoreCase(commandString)) {
            action = WalletAction.SEND;
        }

        if (null == action) {
            return Optional.empty();
        }

        final String player = tokens[1].toLowerCase();

        final String limitString = tokens[2];
        final Double limitValue = Double.valueOf(limitString);

        final WalletCommand newWalletCommand = new WalletCommand(action, 1, player, limitValue);
        return Optional.of(newWalletCommand);
    }
}
