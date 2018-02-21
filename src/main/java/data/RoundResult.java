package data;

import lombok.AllArgsConstructor;
import stockstream.data.OrderResult;
import stockstream.data.TradeCommand;
import stockstream.data.Voter;

import java.util.*;

@AllArgsConstructor
public class RoundResult {
    private Map<Voter, TradeCommand> playerToCommand;
    private SortedMap<TradeCommand, Set<Voter>> rankedTradeCommands;

    private OrderResult orderResult;

    public Optional<OrderResult> getOrderResult() {
        return Optional.ofNullable(orderResult);
    }

    public SortedMap<TradeCommand, Set<Voter>> getRankedTradeCommands() {
        return Collections.unmodifiableSortedMap(rankedTradeCommands);
    }

    public Map<Voter, TradeCommand> getPlayerToCommand() {
        return Collections.unmodifiableMap(playerToCommand);
    }

    @Override
    public String toString() {
        return String.format("playerToCommand:[%s] rankedTradeCommands:[%s]", playerToCommand.size(), rankedTradeCommands.size());
    }
}
