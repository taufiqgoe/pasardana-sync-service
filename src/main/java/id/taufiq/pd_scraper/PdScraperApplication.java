package id.taufiq.pd_scraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PdScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdScraperApplication.class, args);
    }

}
