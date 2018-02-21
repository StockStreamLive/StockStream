package network.gateway.twitch;

import cache.LastOrderCache;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.computer.AssetComputer;
import stockstream.computer.QuoteComputer;
import stockstream.data.Voter;
import stockstream.database.*;
import stockstream.twitch.TwitchChat;
import stockstream.util.TimeUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Responder extends ListenerAdapter {

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private AssetComputer assetComputer;

    @Autowired
    private QuoteComputer quoteComputer;

    @Autowired
    private LastOrderCache lastOrderCache;

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Autowired
    private TwitchChat twitchChat;

    @Override
    public void onMessage(final MessageEvent event) {
        final String eventMessage = event.getMessage();
        if (StringUtils.isEmpty(eventMessage)) {
            return;
        }

        if (!eventMessage.startsWith("!")) {
            return;
        }

        if (event.getUser() == null) {
            return;
        }

        final boolean isSubscriber = "1".equals(event.getTags().getOrDefault("subscriber", "0"));
        final String fromChannel = event.getChannelSource();
        final String sender = event.getUser().getNick();
        final Voter voter = new Voter(sender, "twitch", fromChannel, isSubscriber);

        if (instrumentCache.getValidSymbols().contains(eventMessage.substring(1).toUpperCase())) {
            final String[] parts = eventMessage.split(" ");

            Voter voterToCheck = voter;

            if (parts.length == 2 && parts[1].startsWith("@")) {
                voterToCheck = new Voter(parts[1].substring(1).toLowerCase(), "twitch", fromChannel, false);
            }


            final String symbol = eventMessage.substring(1).toUpperCase();
            final StringBuilder stringBuilder = new StringBuilder();
            if (brokerCache.getSymbolToAsset().containsKey(symbol)) {
                final Asset asset = brokerCache.getSymbolToAsset().get(symbol);
                final double percentReturn = assetComputer.computePercentReturn(asset);
                final double portfolioPercent = (assetComputer.computeAssetValue(asset)/brokerCache.getAccountTotalAssets())*100;
                final int ownedPublicShares = assetComputer.getPublicOwnedPositions(symbol, voterToCheck.getPlayerId()).size();
                final int ownedWalletShares = assetComputer.getWalletOwnedPositions(symbol, voterToCheck.getPlayerId()).size();
                final int ownedTotalShares = ownedPublicShares + ownedWalletShares;

                stringBuilder.append(String.format("%s owns %s of the %s shares of %s @ $%.2f representing %.2f%% of the portfolio with a return of %.2f%%.",
                                                   voterToCheck.getUsername(), ownedTotalShares, asset.getShares(), asset.getSymbol(), asset.getAvgBuyPrice(), portfolioPercent, percentReturn));

            } else {
                stringBuilder.append(String.format("Own 0 shares of %s.", symbol));
            }

            try {
                final Quote stockQuote = assetComputer.loadSymbolToQuote(Collections.singleton(symbol)).get(symbol);
                final double mostRecentPrice = quoteComputer.computeMostRecentPrice(stockQuote);
                final double percentChange = quoteComputer.computePercentChange(stockQuote);

                stringBuilder.append(String.format(" %s last price is around $%.2f. Bid [%s @ $%.2f] Ask [%s @ $%.2f]",
                                                   symbol, mostRecentPrice, stockQuote.getBid_size(), stockQuote.getBid_price(),
                                                   stockQuote.getAsk_size(), stockQuote.getAsk_price()));

                final String changeOperand = percentChange < 0 ? "" : "+";
                stringBuilder.append(String.format(" Today %s%.2f%%.", changeOperand, percentChange));
            } catch (final RobinhoodException e) {
                log.warn("No quote for symbol {}", symbol, e);
            }

            stringBuilder.append(" https://stockstream.live/symbol/").append(symbol);

            twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, stringBuilder.toString()));
        }

        if ("!orders".equalsIgnoreCase(eventMessage)) {
            final StringBuilder stringBuilder = new StringBuilder();

            final List<RobinhoodOrder> pendingOrders = robinhoodOrderRegistry.retrievePendingRobinhoodOrders(TimeUtil.getStartOfToday());
            final List<String> orderStrings = new ArrayList<>();
            pendingOrders.forEach(order -> orderStrings.add(String.format("%s %s @ $%.2f", order.getSide(), order.getSymbol(), Double.valueOf(order.getPrice()))));

            stringBuilder.append(String.format("Have %s pending orders: %s", pendingOrders.size(), orderStrings));

            twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, stringBuilder.toString()));
        }

        if ("!balance".equalsIgnoreCase(eventMessage)) {
            final String response = String.format("Buying Power: $%.2f Net Worth: %.2f Total Assets: %s across %s unique equities.",
                                                  brokerCache.getAccountBalance().getUnallocated_margin_cash(),
                                                  brokerCache.getAccountNetWorth(),
                                                  brokerCache.getAccountTotalAssets(),
                                                  brokerCache.getAssets().size());
            twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, response));

        }

        if (eventMessage.startsWith("!robinhood")) {
            final Wallet wallet = walletRegistry.getWallet(voter.getPlayerId());
            final StringBuilder stringBuilder = new StringBuilder(String.format("@%s ", voter.getUsername()));

            final String[] messageTokens = eventMessage.split(" ");
            if (messageTokens.length == 2) {
                String referralCode = messageTokens[1];
                if (!referralCode.startsWith("http")) {
                    referralCode = "https://" + referralCode;
                }

                boolean validURL = false;
                try {
                    final URI uri = new URI(referralCode);
                    validURL = uri.getHost().toLowerCase().endsWith("robinhood.com");
                } catch (URISyntaxException e) {
                    log.warn(e.getMessage(), e);
                }

                if (validURL) {
                    wallet.setReferralCode(referralCode);
                    walletRegistry.updateWallets(ImmutableList.of(wallet));
                    stringBuilder.append("Success! Active players will have their referral code promoted by StockStream");
                } else {
                    stringBuilder.append("Invalid referral code, you many only share codes from robinhood.com");
                }
            } else {
                stringBuilder.append("Add your Robinhood referral code and it will be included on https://stockstream.live " +
                                     "If you are not signed up yet, visit https://stockstream.live/referral ");
                if (!StringUtils.isEmpty(wallet.getReferralCode())) {
                    stringBuilder.append(String.format("You got %s clicks so far.", wallet.getReferralClicks()));
                } else {
                    stringBuilder.append("To add your referral code enter the command: !robinhood share.robinhood.com_link");
                }
            }

            twitchChat.enqueueMessage(fromChannel, stringBuilder.toString());
        }

        if (eventMessage.equalsIgnoreCase("!last")) {
            final String lastOrderStr = this.lastOrderCache.getLastOrder();

            twitchChat.enqueueMessage(fromChannel, String.format("@%s %s", sender, lastOrderStr));
            return;
        }
    }


}