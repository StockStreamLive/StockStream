package logic.voting;

import data.command.SpeedAction;
import data.command.SpeedCommand;
import logic.game.GameClock;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.logic.elections.Election;

import javax.annotation.PostConstruct;
import java.util.Optional;

public class SpeedElection extends Election<SpeedCommand> {

    @Autowired
    private GameClock gameClock;

    public SpeedElection() {
        super("!speed", SpeedCommand.class, 1);
    }

    @PostConstruct
    public void init() {
        this.withMessageParser(this::parseCommand)
            .withOutcome(new SpeedCommand(SpeedAction.FASTER), this.gameClock::speedUp)
            .withOutcome(new SpeedCommand(SpeedAction.SLOWER), this.gameClock::slowDown);
    }

    public Optional<SpeedCommand> parseCommand(final String command) {
        if (command.equalsIgnoreCase("!faster")) {
            return Optional.of(new SpeedCommand(SpeedAction.FASTER));
        } else if (command.equalsIgnoreCase("!slower")) {
            return Optional.of(new SpeedCommand(SpeedAction.SLOWER));
        }

        return Optional.empty();
    }
}
