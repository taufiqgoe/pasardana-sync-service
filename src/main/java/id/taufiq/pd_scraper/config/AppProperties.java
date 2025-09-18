package id.taufiq.pd_scraper.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank
    private String pasardanaUsername;
    @NotBlank
    private String pasardanaPassword;
    @NotBlank
    private String syncCron;
    private int scrapePoolSize = 20;
}
