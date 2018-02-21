package utils;


import com.google.common.collect.ImmutableList;
import stockstream.util.RandomUtil;

import java.util.Collection;

public class GameUtil {

    private static Collection<String> errorMessages =
            ImmutableList.of("ERROR: {\"message\": \"kappa_rainbow.gif does not exist!\"}",
                             "ERROR: {\"message\": \"cash me outside, how bout dah?\"}",
                             "ERROR: {\"message\": \"PC Load Letter\"}");

    public String getGenericErrorMessage() {
        return RandomUtil.randomChoice(errorMessages).orElse(errorMessages.iterator().next());
    }

}
