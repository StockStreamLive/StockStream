package application.spring;

import data.factory.CommandFactory;
import data.factory.ResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({UtilBeans.class})
@Configuration
public class FactoryBeans {

    @Bean
    public CommandFactory commandFactory() {
        return new CommandFactory();
    }

    @Bean
    public ResponseFactory responseFactory() {
        return new ResponseFactory();
    }

}
