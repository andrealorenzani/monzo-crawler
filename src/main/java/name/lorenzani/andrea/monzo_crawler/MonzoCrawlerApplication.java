package name.lorenzani.andrea.monzo_crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MonzoCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonzoCrawlerApplication.class, args);
    }
}
