package name.lorenzani.andrea.monzo_crawler;

import lombok.extern.log4j.Log4j2;
import name.lorenzani.andrea.monzo_crawler.services.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAsync
@Log4j2
public class MonzoCrawlerApplication implements ApplicationRunner {

	@Autowired
	CrawlerService crawlerService;

	public static void main(String[] args) {
		SpringApplication.run(MonzoCrawlerApplication.class, args);
	}


	// This generates 4 threads running the listenToQueue code - in a prod environment we could autoscale by increasing
	// the instances
	@Override
	public void run(ApplicationArguments args) throws Exception {
		crawlerService.listenToQueue();
		crawlerService.listenToQueue();
		crawlerService.listenToQueue();
		crawlerService.listenToQueue();
	}

	// This is required only to make the shutdown process faster
	@Bean
	public AsyncTaskExecutor asyncTaskExecutor() {
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(20);
		te.setMaxPoolSize(20);
		te.setWaitForTasksToCompleteOnShutdown(true);
		te.setAwaitTerminationSeconds(5);
		return te;
	}
}
