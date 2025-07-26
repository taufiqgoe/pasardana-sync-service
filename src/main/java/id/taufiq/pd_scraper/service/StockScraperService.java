package id.taufiq.pd_scraper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.taufiq.pd_scraper.model.dao.CodeDate;
import id.taufiq.pd_scraper.model.dao.CodePeriodDate;
import id.taufiq.pd_scraper.model.dao.StockReportUpdate;
import id.taufiq.pd_scraper.model.dto.GetStockReport;
import id.taufiq.pd_scraper.model.entity.Stock;
import id.taufiq.pd_scraper.model.entity.StockDaily;
import id.taufiq.pd_scraper.model.entity.StockReport;
import id.taufiq.pd_scraper.repository.CustomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class StockScraperService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CustomRepository customRepository;

    public StockScraperService(ObjectMapper objectMapper, RestClient restClient, CustomRepository customRepository) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.customRepository = customRepository;
    }

    @Scheduled(cron = "#{@appProperties.baseProductSyncCron}")
    private void scrapeStocks() {
        log.info("Starting to scrape stocks");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String getAll = get("https://pasardana.id/api/StockSearchResult/GetAll?pageBegin=1&pageLength=9000&sortField=Code&sortOrder=ASC");
            List<Stock> stocks = objectMapper.readValue(getAll, new TypeReference<>() {
            });
            log.info("Found {} stocks", stocks.size());

            for (Stock stock : stocks) {
                boolean exists = customRepository.existsById(stock.getCode(), Stock.class);
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

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeStockDaily() {
        log.info("Starting to scrape stock daily");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<Stock> stocks = customRepository.findAll(Stock.class);
            log.info("Found {} stocks to scrape for daily data", stocks.size());
            List<CodeDate> maxDatePerCode = customRepository.findAllStockDailyMaxDatePerCode();
            Map<String, LocalDate> maxDatePerCodeMap = maxDatePerCode.stream().collect(toMap(CodeDate::getCode, CodeDate::getDate));
            log.info("Found {} existing daily stock data", maxDatePerCode.size());

            LocalDate now = LocalDate.now();

            stocks.parallelStream().forEach(stock -> {
                try {
                    String code = stock.getCode();
                    LocalDate startDate = maxDatePerCodeMap
                            .getOrDefault(code, LocalDate.of(1995, 1, 1))
                            .plusDays(1);

                    LocalDate endDate = LocalDate.now().plusDays(1);
                    log.debug("Scraping stock daily for code {} from {} to {}", code, startDate, endDate);
                    String stockDataRaw = get("https://pasardana.id/api/StockAPI/GetStockData?code=%s&datestart=%s&dateend=%s".formatted(code, startDate, endDate));
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

                    uniqueStockDailies.forEach(it -> it.setCreatedAt(now));

                    log.debug("Inserting {} stock daily data for code {}", uniqueStockDailies.size(), code);
                    customRepository.insertAll(uniqueStockDailies);
                } catch (Exception e) {
                    log.warn("Failed to fetch stock daily for code {}", stock.getCode());
                }
            });
        } catch (Exception e) {
            log.error("Failed to process stock daily", e);
        }
        logEndTime("stock daily", startTime);
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeStockReport() {
        log.info("Starting to scrape stock report");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<String> codes = customRepository.findAll(Stock.class).stream().map(Stock::getCode).toList();
            log.info("Found {} stock codes to scrape for reports", codes.size());

            String periodsRaw = get("https://pasardana.id/api/StockAPI/GetStockReportPeriods");
            List<String> periods = objectMapper.readValue(periodsRaw, new TypeReference<>() {
            });
            log.info("Found {} report periods", periods.size());

            Map<String, List<String>> currentPeriodsMap = customRepository.findAllCurrentStockReportPeriod();
            log.info("Found {} existing stock reports", currentPeriodsMap.size());

            codes.parallelStream().forEach(code -> {
                try {
                    List<String> current = currentPeriodsMap.getOrDefault(code, List.of());
                    List<String> missingPeriods = periods.stream()
                            .filter(p -> !current.contains(p))
                            .toList();
                    log.debug("Found {} missing periods for code {}", missingPeriods.size(), code);

                    if (missingPeriods.isEmpty()) {
                        return;
                    }

                    String pdStockReportsRaw = get("https://pasardana.id/api/StockAPI/GetStockReports?codes=%s&periods=%s".formatted(code, StringUtils.collectionToCommaDelimitedString(missingPeriods)));
                    List<GetStockReport> pdStockReports = objectMapper.readValue(pdStockReportsRaw, new TypeReference<>() {
                    });

                    for (GetStockReport pdStockReport : pdStockReports) {
                        List<StockReport> stockReports = new ArrayList<>();
                        for (GetStockReport.Detail detail : pdStockReport.getDetails()) {
                            StockReport stockReport = new StockReport(UUID.randomUUID().toString(),
                                    pdStockReport.getCode(),
                                    pdStockReport.getPeriod(),
                                    detail.getPropertyId(),
                                    detail.getValue(),
                                    pdStockReport.getLastUpdate());
                            stockReports.add(stockReport);
                        }

                        log.debug("Inserting {} data code {} period {}", stockReports.size(), code, pdStockReport.getPeriod());
                        customRepository.insertAll(stockReports);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch stock report for code {}", code);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape stock report", e);
        }
        logEndTime("stock report", startTime);
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    public void scrapeStockReportUpdate() {
        log.info("Starting to scrape stock report update");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<String> codes = customRepository.findAll(Stock.class).stream().map(Stock::getCode).toList();
            log.info("Found {} stock codes to check for report updates", codes.size());

            String periodsRaw = get("https://pasardana.id/api/StockAPI/GetStockReportPeriods");
            List<String> periods = objectMapper.readValue(periodsRaw, new TypeReference<>() {
            });
            log.info("Found {} report periods to check", periods.size());

            List<CodePeriodDate> stockReportMaxUpdateDatePerCode = customRepository.findAllStockReportMaxUpdateDatePerCode();
            log.info("Found {} existing stock reports to check for updates", stockReportMaxUpdateDatePerCode.size());

            Map<String, Map<String, CodePeriodDate>> stockReportMaxUpdateDatePerCodeMap = stockReportMaxUpdateDatePerCode.stream()
                    .collect(Collectors.groupingBy(
                            CodePeriodDate::getCode,
                            Collectors.toMap(
                                    CodePeriodDate::getPeriod,
                                    Function.identity(),
                                    (object, object2) -> object
                            )));

            codes.parallelStream().forEach(code -> {
                try {
                    String pdStockReportsRaw = get("https://pasardana.id/api/StockAPI/GetStockReports?codes=%s&periods=%s".formatted(code, StringUtils.collectionToCommaDelimitedString(periods)));
                    List<GetStockReport> pdStockReports = objectMapper.readValue(pdStockReportsRaw, new TypeReference<>() {
                    });
                    for (String period : periods) {
                        GetStockReport pdStockReport = pdStockReports.stream().filter(it -> it.getCode().equalsIgnoreCase(code) && it.getPeriod().equalsIgnoreCase(period)).findFirst().orElse(null);
                        CodePeriodDate codeDate = stockReportMaxUpdateDatePerCodeMap.getOrDefault(code, Map.of()).getOrDefault(period, null);
                        if (pdStockReport != null && pdStockReport.getLastUpdate() != null && codeDate != null) {
                            if (!codeDate.getDate().isEqual(pdStockReport.getLastUpdate().toLocalDate())) {
                                log.debug("Updating stock reports for code {} period {} from date {} to date {}", code, period, codeDate.getDate(), pdStockReport.getLastUpdate().toLocalDate());
                                List<StockReportUpdate> updates = pdStockReport.getDetails().stream()
                                        .map(detail -> new StockReportUpdate(detail.getValue(), pdStockReport.getLastUpdate(), pdStockReport.getCode(), pdStockReport.getPeriod(), detail.getPropertyId()))
                                        .toList();

                                try {
                                    customRepository.batchUpdateStockReports(updates);
                                } catch (Exception e) {
                                    log.error("Failed to update stock report for code {} period {}", pdStockReport.getCode(), pdStockReport.getPeriod());
                                }
                            } else {
                                log.debug("Skipping stock reports for code {} period {} data is up to date {}", code, period, codeDate.getDate());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch stock report update for code {} ", code, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape stock report update", e);
        }
        logEndTime("stock report update", startTime);
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
