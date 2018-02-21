package application;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static application.Stage.LOCAL;

public class Config {

    public static Stage stage = LOCAL;

    public static final float MIN_NET_WORTH = 25000;

    public static final float MAX_INFLUENCED_BUY = 3000;

    public static final Set<String> TWITCH_CHANNELS = ImmutableSet.of("#stockstream", "#moneytesting");

    public static final String RH_UN = System.getenv("ROBINHOOD_USERNAME");
    public static final String RH_PW = System.getenv("ROBINHOOD_PASSWORD");
}
