package application.spring;

import com.cheddar.http.HTTPClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import utils.GameUtil;

@Import({MetricBeans.class})
@Configuration
public class UtilBeans {

    @Bean
    public HTTPClient httpUtil() {
        return new HTTPClient();
    }

    @Bean
    public GameUtil gameUtil() {
        return new GameUtil();
    }

}
