package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import stockstream.data.Voter;

@Data
@AllArgsConstructor
public class Vote {
    private final Voter voter;
    private final String vote;
    private final String fromChannel;
    private final long timestamp;
}
