package application;

import application.spring.ApplicationContext;
import javazoom.jl.decoder.JavaLayerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.IOException;

@Slf4j
public class Application {

    private static AnnotationConfigWebApplicationContext initApplicationContext() {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(ApplicationContext.class);
        context.refresh();
        return context;
    }

    /**
     * !!! Config.stage MUST BE APPLIED BEFORE BEAN INIT !!!
     */
    public static void main(final String[] args) throws JavaLayerException, IOException {
        log.info("StockStream initialized");

        Config.stage = Stage.valueOf(System.getenv().getOrDefault("STAGE", Config.stage.toString()));

        //////////////////////////////////////////////////////////////////

        initApplicationContext();
    }

}
