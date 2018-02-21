package network.gateway.aws;

import cache.LastOrderCache;
import data.RoundResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.CollectionUtils;
import spark.utils.StringUtils;
import stockstream.computer.AssetComputer;
import stockstream.data.OrderResult;
import stockstream.data.TradeCommand;
import stockstream.data.Voter;
import stockstream.database.PlayerVote;
import stockstream.database.PlayerVoteRegistry;
import stockstream.util.TimeUtil;

import java.util.*;

@Slf4j
public class RoundPublisher {

    @Autowired
    private LastOrderCache lastOrderCache;

    @Autowired
    private AssetComputer assetComputer;

    @Autowired
    private PlayerVoteRegistry playerVoteRegistry;

    public void publishRoundResult(final RoundResult roundResult) {
        lastOrderCache.updateLastRoundResult(roundResult);

        log.info("Uploading {} player votes.", roundResult.getPlayerToCommand().size());

        if (CollectionUtils.isEmpty(roundResult.getRankedTradeCommands().keySet())) {
            log.debug("No Commands to publish for round {}.", roundResult);
            return;
        }

        final Map<Voter, TradeCommand> playerCommands = new HashMap<>(roundResult.getPlayerToCommand());

        final Date nowDate = new Date();
        final String primaryKeyDateStr = TimeUtil.getCanonicalMDYString(nowDate);
        final long timeStamp = nowDate.getTime();

        final Collection<PlayerVote> playerVotes = new ArrayList<>();
        playerCommands.forEach((key, value) -> {
            final PlayerVote playerVote = createPlayerVoteFromCommand(key.getPlayerId(), value, timeStamp, primaryKeyDateStr, roundResult.getOrderResult().orElse(null));
            playerVotes.add(playerVote);
        });

        if (CollectionUtils.isEmpty(playerVotes)) {
            log.warn("No voter table records created with round result {}!", roundResult);
            return;
        }

        playerVoteRegistry.savePlayerVotes(playerVotes);
    }

    private PlayerVote createPlayerVoteFromCommand(final String username, final TradeCommand tradeCommand, final long timestamp, final String dateStr, final OrderResult orderResult) {
        String orderId = null;

        if (orderResult.getOrder() != null &&
            orderResult.getAction().equalsIgnoreCase(tradeCommand.getAction().toString()) && orderResult.getSymbol().equalsIgnoreCase(tradeCommand.getParameter())) {
            if (!StringUtils.isEmpty(orderResult.getOrder().getId())) {
                orderId = DigestUtils.sha1Hex(orderResult.getOrder().getId());
            }
        }

        return new PlayerVote(0L, username, dateStr, tradeCommand.getAction().toString(), tradeCommand.getParameter(), timestamp, orderId);
    }


}
