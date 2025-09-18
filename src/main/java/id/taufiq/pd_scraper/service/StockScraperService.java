package id.taufiq.pd_scraper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.taufiq.pd_scraper.model.entity.Stock;
import id.taufiq.pd_scraper.model.entity.StockDaily;
import id.taufiq.pd_scraper.repository.CustomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class StockScraperService {

    private static final String STOCK_SEARCH_ALL_URL = "https://pasardana.id/api/StockSearchResult/GetAll?pageBegin=1&pageLength=9000&sortField=Code&sortOrder=ASC";
    private static final String STOCK_DATA_URL = "https://pasardana.id/api/StockAPI/GetStockData?code=%s&datestart=%s&dateend=%s";
    

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CustomRepository customRepository;
    private final java.util.concurrent.ExecutorService scrapeExecutor;

    public StockScraperService(ObjectMapper objectMapper, RestClient restClient, CustomRepository customRepository, java.util.concurrent.ExecutorService scrapeExecutor) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.customRepository = customRepository;
        this.scrapeExecutor = scrapeExecutor;
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAll() {
        LocalDateTime now = LocalDateTime.now();
        scrapeStocks();
        scrapeStockDaily(now);
    }

    private void scrapeStocks() {
        log.info("Starting to scrape stocks");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String getAll = get(STOCK_SEARCH_ALL_URL);
            List<Stock> stocks = objectMapper.readValue(getAll, new TypeReference<>() {
            });
            log.info("Found {} stocks", stocks.size());

            Set<String> allExistingStockCodes = customRepository.findAllExistingStockCodes();
            log.info("Found {} existing stocks", allExistingStockCodes.size());

            for (Stock stock : stocks) {
                boolean exists = allExistingStockCodes.contains(stock.getCode());
                if (exists) {
                    log.debug("Updating stock {}", stock.getCode());
                    customRepository.update(stock);
                } else {
                    log.debug("Inserting stock {}", stock.getCode());
                    customRepository.insert(stock);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process stock", e);
        }
        logEndTime("stock", startTime);
    }

    private void scrapeStockDaily(LocalDateTime startTime) {
        log.info("Starting to scrape stock daily");

        try {
            Set<String> stockCodes = customRepository.findAllStockCodes();

            List<Callable<Void>> tasks = new ArrayList<>();

            Map<String, LocalDate> maxDatePerCodeMap = customRepository.findAllStockDailyMaxDatePerCode();

            for (String code : stockCodes) {
                tasks.add(() -> {
                    try {
                        LocalDate startDate = maxDatePerCodeMap
                                .getOrDefault(code, LocalDate.of(1995, 1, 1))
                                .plusDays(1);

                        LocalDate endDate = startTime.toLocalDate().plusDays(1);
                        log.debug("Scraping stock daily for code {} from {} to {}", code, startDate, endDate);
                        String stockDataRaw = get(String.format(STOCK_DATA_URL, code, startDate, endDate));
                        List<StockDaily> stockDailies = objectMapper.readValue(stockDataRaw, new TypeReference<>() {
                        });

                        List<StockDaily> uniqueStockDailies = new ArrayList<>(
                                stockDailies.stream()
                                        .collect(toMap(
                                                sd -> sd.getCode() + "|" + sd.getDate(),
                                                Function.identity(),
                                                (existing, replacement) -> existing
                                        ))
                                        .values());

                        uniqueStockDailies.forEach(it -> it.setCreatedAt(startTime.toLocalDate()));

                        log.debug("Inserting {} stock daily data for code {}", uniqueStockDailies.size(), code);
                        customRepository.insertAll(uniqueStockDailies);
                    } catch (Exception e) {
                        log.warn("Failed to fetch stock daily for code {}", code);
                    }
                    return null;
                });
            }

                List<Future<Void>> futures = scrapeExecutor.invokeAll(tasks);
                for (Future<Void> f : futures) {
                    try { f.get(); } catch (Exception ignored) {}
                }
            
            
        } catch (Exception e) {
            log.error("Failed to process stock daily", e);
        }
        logEndTime("stock daily", startTime);
    }

    private String get(String endpoint) {
        ResponseEntity<byte[]> entity = restClient.get()
                .uri(endpoint)
                .retrieve()
                .toEntity(byte[].class);

        if (entity.getBody() == null) {
            throw new RuntimeException();
        }

        return new String(entity.getBody(), StandardCharsets.UTF_8);
    }

    private void logEndTime(String event, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        log.info("Finished scraping {} with time spent: {} minutes {} seconds",
                event, minutes, seconds);
    }
}
