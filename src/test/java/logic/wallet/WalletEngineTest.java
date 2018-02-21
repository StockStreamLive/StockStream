package logic.wallet;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.Order;
import com.cheddar.robinhood.exception.RobinhoodException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.computer.OrderComputer;
import stockstream.data.*;
import stockstream.database.*;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class WalletEngineTest {

    @Mock
    private OrderComputer orderComputer;

    @Mock
    private WalletRegistry walletRegistry;

    @Mock
    private WalletOrderRegistry walletOrderRegistry;

    @Mock
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Mock
    private RobinhoodAPI broker;

    @InjectMocks
    private WalletEngine walletEngine;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecuteWalletCommand_buyOrderNotNull_expectOrderArchived() throws RobinhoodException {
        final Order expectedOrder = new Order();
        expectedOrder.setId("123");

        when(broker.buyShares(any(), anyInt(), anyDouble())).thenReturn(expectedOrder);

        final Order returnedOrder = walletEngine.executeWalletCommand(new WalletCommand(WalletAction.BUY, 1, "AMZN", 900d), new Wallet("twitch:michrob", 5000d, 0d, 0d));

        assertEquals(expectedOrder, returnedOrder);
        verify(walletOrderRegistry, times(1)).saveWalletOrder(any());
        verify(robinhoodOrderRegistry, times(1)).saveRobinhoodOrder(any());
    }

    @Test
    public void testExecuteWalletCommand_sellOrderNotNull_expectOrderArchived() throws RobinhoodException {
        final Order expectedOrder = new Order();
        expectedOrder.setId("123");

        when(broker.sellShares(any(), anyInt(), anyDouble())).thenReturn(expectedOrder);
        when(walletOrderRegistry.findNextSellableBuyOrder(any(), any())).thenReturn(Optional.of(new WalletOrder()));

        final Order returnedOrder = walletEngine.executeWalletCommand(new WalletCommand(WalletAction.SELL, 1, "AMZN", 900d), new Wallet("twitch:michrob", 5000d, 0d, 0d));

        assertEquals(expectedOrder, returnedOrder);
        verify(walletOrderRegistry, times(1)).saveWalletOrder(any());
        verify(robinhoodOrderRegistry, times(1)).saveRobinhoodOrder(any());
    }

    @Test
    public void testProcessWalletCommand_sellOrderNoShares_expectNoOrdersArchived() throws RobinhoodException, ExecutionException {
        final Order expectedOrder = new Order();
        expectedOrder.setId("123");

        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 2000, 0d, 0d));
        when(broker.sellShares(any(), anyInt(), anyDouble())).thenReturn(expectedOrder);
        when(walletOrderRegistry.findNextSellableBuyOrder(any(), any())).thenReturn(Optional.of(new WalletOrder()));
        when(orderComputer.preProcessWalletCommand(any(), any())).thenReturn(OrderStatus.NO_SHARES);

        final WalletOrderResult orderResult = walletEngine.processQuantityWalletOrder(new WalletCommand(WalletAction.SELL, 1, "AMZN", 900d),
                                                                                new Voter("michrob", "twitch", "#stockstream", true));

        assertEquals(OrderStatus.NO_SHARES, orderResult.getOrderStatus());
        verify(walletOrderRegistry, times(0)).saveWalletOrder(any());
        verify(robinhoodOrderRegistry, times(0)).saveRobinhoodOrder(any());
    }

    @Test
    public void testProcessWalletCommand_quantitySellOrderOk_expectOrdersArchived() throws RobinhoodException, ExecutionException {
        final Order expectedOrder = new Order();
        expectedOrder.setId("123");

        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 2000, 0d, 0d));
        when(broker.buyShares(any(), anyInt(), anyDouble())).thenReturn(expectedOrder);
        when(walletOrderRegistry.findNextSellableBuyOrder(any(), any())).thenReturn(Optional.of(new WalletOrder()));
        when(orderComputer.preProcessWalletCommand(any(), any())).thenReturn(OrderStatus.OK);

        final WalletOrderResult walletOrderResult = walletEngine.processQuantityWalletOrder(new WalletCommand(WalletAction.BUY, 5, "AMZN", 900d),
                                                                                            new Voter("michrob", "twitch", "#stockstream", true));

        assertEquals(OrderStatus.OK, walletOrderResult.getOrderStatus());
        verify(walletOrderRegistry, times(5)).saveWalletOrder(any());
        verify(robinhoodOrderRegistry, times(5)).saveRobinhoodOrder(any());
    }

    @Test
    public void testProcessWalletCommand_quantitySellOrderNotEnough_expectOrdersNotArchived() throws RobinhoodException, ExecutionException {
        final Order expectedOrder = new Order();
        expectedOrder.setId("123");

        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 2000, 0d, 0d));
        when(broker.sellShares(any(), anyInt(), anyDouble())).thenReturn(expectedOrder);
        when(walletOrderRegistry.findNextSellableBuyOrder(any(), any())).thenReturn(Optional.of(new WalletOrder()));
        when(orderComputer.preProcessWalletCommand(any(), any())).thenReturn(OrderStatus.OK);

        final WalletOrderResult walletOrderResult = walletEngine.processQuantityWalletOrder(new WalletCommand(WalletAction.SELL, 5, "AMZN", 900d),
                                                                                            new Voter("michrob", "twitch", "#stockstream", true));

        assertEquals(OrderStatus.OK, walletOrderResult.getOrderStatus());
        verify(walletOrderRegistry, times(5)).updateWalletOrder(any());
        verify(robinhoodOrderRegistry, times(5)).saveRobinhoodOrder(any());
    }

    @Test
    public void testProcessWalletCommand_sellOrderOk_expectOrdersArchived() throws RobinhoodException, ExecutionException {
        final Order expectedOrder = new Order();
        expectedOrder.setId("123");

        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 2000, 0d, 0d));
        when(broker.sellShares(any(), anyInt(), anyDouble())).thenReturn(expectedOrder);
        when(walletOrderRegistry.findNextSellableBuyOrder(any(), any())).thenReturn(Optional.of(new WalletOrder()));
        when(orderComputer.preProcessWalletCommand(any(), any())).thenReturn(OrderStatus.OK);

        final OrderResult orderResult = walletEngine.processWalletCommand("twitch:michrob", new WalletCommand(WalletAction.SELL, 1, "AMZN", 900d));

        assertEquals(OrderStatus.OK, orderResult.getOrderStatus());
        verify(walletOrderRegistry, times(1)).updateWalletOrder(any());
        verify(robinhoodOrderRegistry, times(1)).saveRobinhoodOrder(any());
    }
}
