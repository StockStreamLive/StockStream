package cache;


import data.RoundResult;
import data.factory.ResponseFactory;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.twitch.TwitchChat;
import utils.GameUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LastOrderCache {

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    private GameUtil gameUtil;

    @Autowired
    private TwitchChat twitchChat;

    @Getter
    private String lastOrder = "";

    @Getter
    private Map<String, String> playerToLastOrder = new ConcurrentHashMap<>();

    public void setLastOrderForPlayer(final String player, final String response) {
        playerToLastOrder.put(player, response);
    }

    public void updateLastRoundResult(final RoundResult roundResult) {
        if (!roundResult.getOrderResult().isPresent()) {
            lastOrder = "";
        }

        int votesReceived = 0;
        if (roundResult.getRankedTradeCommands().size() > 0) {
            votesReceived = roundResult.getRankedTradeCommands().values().iterator().next().size();
        }

        lastOrder = responseFactory.constructResponse(roundResult.getOrderResult().get(), votesReceived);

        twitchChat.broadcastMessage(lastOrder);
    }


}
