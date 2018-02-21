package logic.wallet;

import cache.LastOrderCache;
import com.cheddar.robinhood.exception.RobinhoodException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.computer.AssetComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.Voter;
import stockstream.database.Asset;
import stockstream.database.Wallet;
import stockstream.database.WalletRegistry;
import stockstream.twitch.TwitchChat;

import java.util.Collection;

@Slf4j
public class WalletBot extends ListenerAdapter {

    @Autowired
    private AssetComputer assetComputer;

    @Autowired
    private LastOrderCache lastOrderCache;

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletComputer walletComputer;

    @Autowired
    private TwitchChat twitchChat;

    @Override
    public void onMessage(final MessageEvent event) {
        final String eventMessage = event.getMessage();
        if (StringUtils.isEmpty(eventMessage)) {
            return;
        }

        if (!eventMessage.startsWith("#") && !eventMessage.startsWith("!")) {
            return;
        }

        if (event.getUser() == null) {
            return;
        }

        final boolean isSubscriber = "1".equals(event.getTags().getOrDefault("subscriber", "0"));
        final String fromChannel = event.getChannelSource();
        final String sender = event.getUser().getNick();
        final Voter voter = new Voter(sender, "twitch", fromChannel, isSubscriber);

        if (eventMessage.equalsIgnoreCase("#") ||
            eventMessage.equalsIgnoreCase("#w") ||
            eventMessage.equalsIgnoreCase("!score")) {
            try {
                final String walletResponse = constructWalletResponse(voter);
                twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, walletResponse));
            } catch (RobinhoodException e) {
                log.warn(e.getMessage(), e);
                return;
            }
        }

        if (eventMessage.equalsIgnoreCase("#last")) {
            final String response = lastOrderCache.getPlayerToLastOrder().getOrDefault("twitch:" + sender, "");
            if (!StringUtils.isEmpty(response)) {
                twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, response));
            }
        }

        if (eventMessage.startsWith("#") ||
            eventMessage.startsWith("#w") ||
            eventMessage.startsWith("#wallet") ||
            eventMessage.startsWith("!score")) {
            final String[] parts = eventMessage.split(" ");

            if (parts.length == 2 && parts[1].startsWith("@")) {
                try {
                    final Voter voterToCheck = new Voter(parts[1].substring(1).toLowerCase(), "twitch", fromChannel, false);

                    final String walletResponse = constructWalletResponse(voterToCheck);
                    twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, walletResponse));
                } catch (RobinhoodException e) {
                    log.warn(e.getMessage(), e);
                    return;
                }
            }
        }

    }

    public String constructWalletResponse(final Voter voter) throws RobinhoodException {
        try {
            final Wallet playerWallet = walletRegistry.getWallet(voter.getPlayerId());
            final double votingBalance = walletComputer.computeBuyingPower(playerWallet);
            final double walletBalance = walletComputer.computeSpendingBalance(playerWallet);
            final Collection<Asset> assetsForPlayer = assetComputer.getAssetsOwnedByPlayer(voter.getPlayerId());

            final String playerProfileURL = "https://stockstream.live/player/" + voter.getPlayerId();

            return String.format("Wallet of %s: Voting power: $%.2f Wallet balance: $%.2f Assets: %s Profile: %s",
                                 voter.getUsername(), votingBalance, walletBalance, assetsForPlayer, playerProfileURL);
        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
        return "";
    }

}