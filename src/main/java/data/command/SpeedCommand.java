package data.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import stockstream.logic.elections.Candidate;

@Data
@AllArgsConstructor
public class SpeedCommand implements Candidate {

    private SpeedAction speedAction;

    @Override
    public String getLabel() {
        return StringUtils.capitalize(speedAction.name().toLowerCase());
    }

}
