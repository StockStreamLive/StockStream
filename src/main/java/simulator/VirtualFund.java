package simulator;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.client.RobinhoodClient;
import com.cheddar.robinhood.data.*;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import data.Event;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.InstrumentCache;
import stockstream.computer.QuoteComputer;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;
import stockstream.util.RandomUtil;
import stockstream.util.TimeUtil;

import java.util.*;


@Slf4j
public class VirtualFund implements RobinhoodAPI {

    @Autowired
    private RobinhoodClient robinhoodClient;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private QuoteComputer quoteComputer;

    @Autowired
    private InstrumentCache instrumentCache;

    private final Map<String, Position> positions = new HashMap<>();
    private final List<Order> orders = new ArrayList<>();

    private double unallocated_margin_cash = 50000.0;
    private double margin_limit = 2000.0;

    private double getModdedPrice(final String symbol) throws RobinhoodException {
        final Quote quote = getQuote(symbol);

        double price = quoteComputer.computeMostRecentPrice(quote);
        double modifier = price + (RandomUtil.randomChoice(ImmutableList.of(.01, .02, .03, .04)).get() * RandomUtil.choice(-1, 1));

        return modifier;
    }

    @Override
    public MarginBalances getMarginBalances() {
        return new MarginBalances(unallocated_margin_cash, margin_limit, 1234567.89, 4321.0);
    }

    @Override
    public List<Position> getPositions() throws RobinhoodException {
        return new ArrayList<>(positions.values());
    }

    @Override
    public Collection<Order> getOrdersAfterDate(final Date date) throws RobinhoodException {
        return orders;
    }

    @Override
    public List<EquityHistorical> getHistoricalValues(final String span, final String interval, final String bounds) throws RobinhoodException {
        return robinhoodClient.getHistoricalValues(span, interval, bounds);
    }

    @Override
    public List<Quote> getQuotes(final Collection<String> symbols) throws RobinhoodException {
        final List<Quote> quotes = robinhoodClient.getQuotes(symbols);
        return quotes;
    }

    @Override
    public Quote getQuote(final String symbol) throws RobinhoodException {
        return robinhoodClient.getQuote(symbol);
    }

    @Override
    public Order buyShares(final String symbol, final int shares, final double limit) throws RobinhoodException {
        final double price = getModdedPrice(symbol);

        final InstrumentStub instrument = instrumentCache.getSymbolToInstrument().get(symbol);

        final Position position = positions.computeIfAbsent(symbol, newAsset -> new Position());

        double ownedShares = position.getQuantity();

        double currentTotalPrice = ownedShares * position.getAverage_buy_price();

        ownedShares = ownedShares + shares;
        this.unallocated_margin_cash -= (price * shares);

        double newTotalPrice = (price * shares) + currentTotalPrice;
        double newAverage = newTotalPrice / ownedShares;

        position.setAverage_buy_price(newAverage);
        position.setQuantity(ownedShares);
        position.setInstrument(instrument.getUrl());

        final String uuid = UUID.randomUUID().toString();
        final String dateStr = TimeUtil.createStrFromDate(new Date(), "yyyy-MM-dd'T'HH:mm:ssX");

        final Order placedOrder =
                new Order(uuid, RandomUtil.randomChoice(ImmutableSet.of("filled","confirmed","unconfirmed", "queued")).get(), dateStr, String.valueOf(price), String.valueOf(limit),
                          "", "buy", instrument.getUrl(), String.valueOf(shares), ImmutableList.of(new Execution("1", dateStr)));

        orders.add(placedOrder);

        return placedOrder;
    }

    @Override
    public Order sellShares(final String symbol, final int shares, final double limit) throws RobinhoodException {
        final double price = getModdedPrice(symbol);

        if (!positions.containsKey(symbol)) {
            throw new RobinhoodException("No share of " + symbol);
        }

        final InstrumentStub instrument = instrumentCache.getSymbolToInstrument().get(symbol);

        Position position = positions.get(symbol);

        double ownedShares = position.getQuantity();

        ownedShares = ownedShares - 1;

        if (ownedShares <= 0) {
            positions.remove(symbol);
        }

        position.setQuantity(ownedShares);

        this.unallocated_margin_cash += price;

        this.scheduler.notifyEvent(Event.ORDER_PLACED);

        final String uuid = UUID.randomUUID().toString();
        final String dateStr = TimeUtil.createStrFromDate(new Date(), "yyyy-MM-dd'T'HH:mm:ssX");

        final Order placedOrder =
                new Order(uuid, "filled", dateStr, String.valueOf(price), String.valueOf(limit),
                          "", "sell", instrument.getUrl(), String.valueOf(shares), ImmutableList.of(new Execution("1", dateStr)));

        orders.add(placedOrder);

        return placedOrder;
    }

    @Override
    public Optional<Order> getOrderFromURL(final String orderURL) throws RobinhoodException {
        return robinhoodClient.getOrderFromURL(orderURL);
    }

    @Override
    public MarketState getMarketStateForDate(final DateTime inputTime) throws RobinhoodException {
        final boolean openThisDay = true;/*
                !(inputTime.getDayOfWeek() == DateTimeConstants.SATURDAY || inputTime.getDayOfWeek() == DateTimeConstants.SUNDAY);*/
        final DateTime marketOpen = new DateTime().withDayOfYear(inputTime.getDayOfYear())
                .withHourOfDay(0).withMinuteOfHour(0);
        final DateTime marketClose = new DateTime().withDayOfYear(inputTime.getDayOfYear())
                .withHourOfDay(23).withMinuteOfHour(59);

        final MarketState marketState =
                new MarketState(ImmutableMap.of("is_open", String.valueOf(openThisDay),
                        "opens_at", marketOpen.toString(ISODateTimeFormat.dateTimeNoMillis()),
                        "extended_opens_at", marketOpen.toString(ISODateTimeFormat.dateTimeNoMillis()),
                        "closes_at", marketClose.toString(ISODateTimeFormat.dateTimeNoMillis()),
                        "extended_closes_at", marketClose.toString(ISODateTimeFormat.dateTimeNoMillis())));
        return marketState;
    }

    @Override
    public Portfolio getPortfolio() throws RobinhoodException {
        return new Portfolio(0, 0, 0, 4321.0, 4321.0);
    }

    public Set<Instrument> getAllInstruments() {
        return robinhoodClient.getAllInstruments();
    }

    @Override
    public Optional<Instrument> getInstrumentFromURL(final String instrumentURL) {
        return robinhoodClient.getInstrumentFromURL(instrumentURL);
    }

    @Override
    public Optional<Instrument> getInstrumentForSymbol(final String symbol) {
        return robinhoodClient.getInstrumentForSymbol(symbol);
    }
}