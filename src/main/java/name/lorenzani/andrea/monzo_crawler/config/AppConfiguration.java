package name.lorenzani.andrea.monzo_crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class AppConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
