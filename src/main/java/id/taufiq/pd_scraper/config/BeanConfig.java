package id.taufiq.pd_scraper.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class BeanConfig {

    private final AppProperties appProperties;

    public BeanConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        String credentials = appProperties.getPasardanaUsername() + ":" + appProperties.getPasardanaPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        return RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .defaultHeader(HttpHeaders.HOST, "pasardana.id")
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .requestFactory(factory)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService scrapeExecutor() {
        int poolSize = Math.max(1, appProperties.getScrapePoolSize());
        return Executors.newFixedThreadPool(poolSize);
    }
}
