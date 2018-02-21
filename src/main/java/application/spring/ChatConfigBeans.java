package application.spring;

import application.Config;
import org.pircbotx.cap.EnableCapHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({DispatcherBeans.class,
         EngineBeans.class})
@Configuration
public class ChatConfigBeans {

    @Autowired
    private DispatcherBeans dispatcherBeans;

    @Autowired
    private EngineBeans engineBeans;

    // TEST CONFIG
    public org.pircbotx.Configuration testChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setName("stockstream")
                                                       .addServer("localhost")
                                                       .addAutoJoinChannels(Config.TWITCH_CHANNELS)
                                                       .addListener(dispatcherBeans.walletBot())
                                                       .addListener(dispatcherBeans.responder())
                                                       .addListener(engineBeans.voteEngine())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    // LOCAL CONFIG
    public org.pircbotx.Configuration localChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setName("stockstream")
                                                       .addServer("localhost")
                                                       .addAutoJoinChannels(Config.TWITCH_CHANNELS)
                                                       .addListener(dispatcherBeans.walletBot())
                                                       .addListener(dispatcherBeans.responder())
                                                       .addListener(engineBeans.voteEngine())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    // GAMMA CONFIG
    public org.pircbotx.Configuration gammaChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setCapEnabled(true)
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/commands"))
                                                       .setName("stockstream")
                                                       .setServerPassword("oauth:wee12rhgskga3qf1iq4rbvq0f61i7s")
                                                       .addServer("irc.chat.twitch.tv")
                                                       .addAutoJoinChannel("#michrob")
                                                       .addListener(dispatcherBeans.walletBot())
                                                       .addListener(dispatcherBeans.responder())
                                                       .addListener(engineBeans.voteEngine())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    // PROD CONFIG
    public org.pircbotx.Configuration prodChannelConfiguration() {
        return new org.pircbotx.Configuration.Builder().setAutoNickChange(false)
                                                       .setOnJoinWhoEnabled(false)
                                                       .setCapEnabled(true)
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
                                                       .addCapHandler(new EnableCapHandler("twitch.tv/commands"))
                                                       .setName("stockstream")
                                                       .setServerPassword("oauth:wee12rhgskga3qf1iq4rbvq0f61i7s")
                                                       .addServer("irc.chat.twitch.tv")
                                                       .addAutoJoinChannels(Config.TWITCH_CHANNELS)
                                                       .addListener(dispatcherBeans.walletBot())
                                                       .addListener(dispatcherBeans.responder())
                                                       .addListener(engineBeans.voteEngine())
                                                       .setAutoReconnect(true)
                                                       .setAutoReconnectDelay(5 * 1000)
                                                       .buildConfiguration();
    }

    @Bean
    public org.pircbotx.Configuration configuration() {
        switch (Config.stage) {
            case TEST: {
                return testChannelConfiguration();
            } case LOCAL:{
                return localChannelConfiguration();
            } case GAMMA: {
                return gammaChannelConfiguration();
            } case PROD: {
                return prodChannelConfiguration();
            } default: {
                return testChannelConfiguration();
            }
        }
    }

}
