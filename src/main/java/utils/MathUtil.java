package utils;


public class MathUtil {

    public static double computePercentChange(final double valueThen, final double valueNow) {
        final double change = valueNow - valueThen;

        return change / valueThen * 100;
    }

}
