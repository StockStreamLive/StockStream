package network.gateway.aws;

import application.AWSConfig;
import application.Config;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.InstrumentCache;
import stockstream.data.OrderResult;
import stockstream.database.RobinhoodOrder;
import stockstream.database.RobinhoodOrderRegistry;
import stockstream.logic.PubSub;
import stockstream.util.JSONUtil;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Slf4j
public class OrderPublisher {

    @Autowired
    private PubSub pubSub;

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @PostConstruct
    public void init() {
        pubSub.subscribeFunctionToClassType(this::publishOrderResult, OrderResult.class);
    }

    private Void publishOrderResult(final OrderResult orderResult) {
        if (orderResult.getAction() == null) {
            return null;
        }

        publishToSNS(orderResult);
        publishToRegistry(orderResult);

        return null;
    }

    private void publishToRegistry(final OrderResult orderResult) {
        if (orderResult.getOrder() == null) {
            return;
        }
        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder(orderResult.getSymbol(), orderResult.getOrder());
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);
    }

    private void publishToSNS(final OrderResult orderResult) {
        final String order = orderResult.getAction() + " " + orderResult.getSymbol();

        final Optional<String> jsonOrder = JSONUtil.serializeObject(order, false);

        if (!jsonOrder.isPresent()) {
            log.warn("Order result {} -> {}. Could not convert to JSON!", orderResult, order);
            return;
        }

        final AmazonSNSAsync snsClient = AmazonSNSAsyncClientBuilder.standard().withRegion(AWSConfig.AWS_REGION).withCredentials(AWSConfig.PROVIDER).build();
        snsClient.publish(AWSConfig.ORDERS_SNS_ARN_MAP.get(Config.stage), jsonOrder.get(), "$");
    }

}
