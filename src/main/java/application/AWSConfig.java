package application;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class AWSConfig {

    private static final String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
    private static final String AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY");

    public static final AWSCredentialsProvider PROVIDER = new AWSCredentialsProvider() {
        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return AWS_ACCESS_KEY;
                }

                @Override
                public String getAWSSecretKey() {
                    return AWS_SECRET_KEY;
                }
            };
        }

        @Override
        public void refresh() {}
    };

    public static final Regions AWS_REGION = Regions.US_EAST_1;

    public static final Map<Stage, String> ORDERS_SNS_ARN_MAP =
            ImmutableMap.of(Stage.TEST, "arn:aws:sns:us-east-1:237086950555:StockStreamOrdersBeta",
                            Stage.LOCAL, "arn:aws:sns:us-east-1:237086950555:StockStreamOrdersBeta",
                            Stage.GAMMA, "arn:aws:sns:us-east-1:237086950555:StockStreamOrdersBeta",
                            Stage.PROD, "arn:aws:sns:us-east-1:237086950555:StockStreamOrders");

    public static final Map<Stage, String> LOCKS_DYNAMODB_TABLE_MAP =
            ImmutableMap.of(Stage.TEST, "StockStreamSymbolLocksTest",
                            Stage.LOCAL, "StockStreamSymbolLocksTest",
                            Stage.GAMMA, "StockStreamSymbolLocksTest",
                            Stage.PROD, "StockStreamSymbolLocksProd");
}
