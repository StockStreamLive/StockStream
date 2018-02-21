package logic.wallet;

import cache.LastOrderCache;
import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.Order;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import data.factory.ResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.computer.OrderComputer;
import stockstream.computer.WalletComputer;
import stockstream.data.*;
import stockstream.database.*;
import stockstream.twitch.TwitchChat;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
public class WalletEngine {

    @Autowired
    private OrderComputer orderComputer;

    @Autowired
    private RobinhoodAPI broker;

    @Autowired
    private LastOrderCache lastOrderCache;

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletOrderRegistry walletOrderRegistry;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Autowired
    private TwitchChat twitchChat;

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    private WalletComputer walletComputer;

    public synchronized void processWalletCommandFromVoter(final WalletCommand walletCommand, final Voter voter) {
        try {
            final WalletOrderResult walletOrderResult = processQuantityWalletOrder(walletCommand, voter);

            final String response = responseFactory.constructWalletCommandResponse(voter.getPlayerId(), walletOrderResult);

            twitchChat.enqueueMessage(voter.getChannel(), String.format("@%s %s", voter.getUsername(), response));
            lastOrderCache.setLastOrderForPlayer(voter.getPlayerId(), response);

        } catch (final RobinhoodException | ExecutionException e) {
            log.warn("{} {} -> {}", voter, walletCommand, e.getMessage(), e);
        }
    }

    @VisibleForTesting
    protected WalletOrderResult processQuantityWalletOrder(final WalletCommand walletCommand, final Voter voter) throws ExecutionException, RobinhoodException {
        OrderStatus orderStatus = orderComputer.preProcessWalletCommand(voter.getPlayerId(), walletCommand);

        if (!OrderStatus.OK.equals(orderStatus)) {
            return new WalletOrderResult(walletCommand, 0, orderStatus);
        }

        int completedOrders = 0;

        for (int i = 0; i < walletCommand.getQuantity(); ++i) {
            final OrderResult orderResult = processWalletCommand(voter.getPlayerId(), walletCommand);
            orderStatus = orderResult.getOrderStatus();

            if (!OrderStatus.OK.equals(orderResult.getOrderStatus())) {
                break;
            }

            completedOrders++;
        }

        return new WalletOrderResult(walletCommand, completedOrders, orderStatus);
    }

    @VisibleForTesting
    protected OrderResult processWalletCommand(final String player, final WalletCommand walletCommand) throws RobinhoodException, ExecutionException {
        final Wallet wallet = walletRegistry.getWallet(player);

        final OrderResult orderResult = new OrderResult(walletCommand.getAction().toString(), walletCommand.getParameter(), OrderStatus.OK, null);

        if (!OrderStatus.OK.equals(orderResult.getOrderStatus())) {
            return orderResult;
        }

        final Order order = executeWalletCommand(walletCommand, wallet);
        orderResult.setOrder(order);

        return orderResult;
    }

    @VisibleForTesting
    protected Order executeWalletCommand(final WalletCommand walletCommand, final Wallet wallet) throws RobinhoodException {
        Order order = null;

        switch (walletCommand.getAction()) {
            case BUY: {
                order = broker.buyShares(walletCommand.getParameter(), 1, walletCommand.getLimit());
                break;
            } case SELL: {

                final Optional<WalletOrder> buyOrder = walletOrderRegistry.findNextSellableBuyOrder(wallet.getPlatform_username(), walletCommand.getParameter());

                if (!buyOrder.isPresent()) {
                    log.error("Can't sell with no shares to sell! {} {}", walletCommand, wallet);
                    return null;
                }

                order = broker.sellShares(walletCommand.getParameter(), 1, walletCommand.getLimit());

                buyOrder.get().setSell_order_id(DigestUtils.sha1Hex(order.getId()));
                walletOrderRegistry.updateWalletOrder(buyOrder.get());

                break;
            } case SEND: {

                String targetPlayer = walletCommand.getParameter();
                if (targetPlayer.startsWith("@")) {
                    targetPlayer = targetPlayer.substring(1);
                }

                final Wallet targetWallet = walletRegistry.getWallet("twitch:" + targetPlayer);

                wallet.setSentDollars(wallet.getSentDollars() + walletCommand.getLimit());
                targetWallet.setReceivedDollars(targetWallet.getReceivedDollars() + walletCommand.getLimit());

                walletRegistry.updateWallets(ImmutableSet.of(wallet, targetWallet));

            } default: {
                break;
            }
        }

        if (order != null) {
            final WalletOrder walletOrder = new WalletOrder(walletCommand.getParameter(), order, wallet.getPlatform_username());
            final RobinhoodOrder robinhoodOrder = new RobinhoodOrder(walletCommand.getParameter(), order);

            walletOrderRegistry.saveWalletOrder(walletOrder);
            robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);
        }

        return order;
    }

}
