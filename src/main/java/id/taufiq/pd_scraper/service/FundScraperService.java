package id.taufiq.pd_scraper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.taufiq.pd_scraper.model.dao.CodeDate;
import id.taufiq.pd_scraper.model.entity.Fund;
import id.taufiq.pd_scraper.model.entity.FundAum;
import id.taufiq.pd_scraper.model.entity.FundDaily;
import id.taufiq.pd_scraper.model.entity.FundUnit;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class FundScraperService {

    private static final String FUND_SEARCH_URL = "https://pasardana.id/api/FundAPI/SearchFund";
    private static final String FUND_NAV_HISTORIC_URL = "https://pasardana.id/api/FundAPI/GetFundNAVHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s";
    private static final String FUND_AUM_HISTORIC_URL = "https://pasardana.id/api/FundAPI/GetFundAUMHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s";
    private static final String FUND_UNIT_HISTORIC_URL = "https://pasardana.id/api/FundAPI/GetFundUPHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CustomRepository customRepository;
    private static final int DEFAULT_SCRAPE_POOL_SIZE = 20;
    private final ExecutorService scrapeExecutor = Executors.newFixedThreadPool(DEFAULT_SCRAPE_POOL_SIZE);

    public FundScraperService(ObjectMapper objectMapper, RestClient restClient, CustomRepository customRepository) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.customRepository = customRepository;
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAll() {
        LocalDateTime startTime = LocalDateTime.now();

        // fetch fund ids once and pass to downstream methods to avoid repeated DB calls
        Set<Integer> allFundIds = null;
        try {
            allFundIds = customRepository.findAllFundIds();
        } catch (Exception e) {
            log.warn("Failed to fetch funds upfront, methods will fallback to individual fetch", e);
        }

        scrapeFunds();
        scrapeAllFundNavDaily(startTime, allFundIds);
        scrapeAllFundAumDaily(startTime, allFundIds);
        scrapeAllFundUnitDaily(startTime, allFundIds);
    }

    private void scrapeFunds() {
        log.info("Starting to scrape funds");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String getAll = get(FUND_SEARCH_URL);
            List<Fund> funds = objectMapper.readValue(getAll, new TypeReference<List<Fund>>() {
                    }).stream()
                    .filter(it -> it.getId() != null && it.getId() > 0)
                    .sorted(Comparator.comparing(Fund::getId))
                    .toList();
            log.info("Found {} funds", funds.size());

            Set<Integer> existingFundIds = customRepository.findAllExistingFundIds();
            log.info("Found {} existing funds", existingFundIds.size());

            List<Fund> updateFunds = new ArrayList<>();
            List<Fund> insertFunds = new ArrayList<>();

            funds.forEach(fund -> {
                try {
                    boolean exists = existingFundIds.contains(fund.getId());
                    if (exists) {
                        log.debug("Updating fund {}", fund.getId());
                        updateFunds.add(fund);
                    } else {
                        log.debug("Inserting fund {}", fund.getId());
                        insertFunds.add(fund);
                    }
                } catch (Exception e) {
                    log.warn("Failed to upsert fund for id {}", fund.getId(), e);
                }
            });

            log.info("Updating {} funds and inserting {} funds", updateFunds.size(), insertFunds.size());
            customRepository.updateAll(updateFunds);
            customRepository.insertAll(insertFunds);
        } catch (Exception e) {
            log.error("Failed to scrape funds", e);
        }
        logEndTime("funds", startTime);
    }

    private void scrapeAllFundNavDaily(LocalDateTime startTime, Set<Integer> fundIdsParam) {
        log.info("Starting to scrape all fund nav daily");

        try {
            Set<Integer> fundIds = fundIdsParam;
            if (fundIds == null) {
                fundIds = customRepository.findAllFundIds();
            }
            log.info("Found {} funds to scrape for daily nav", fundIds.size());

            Map<Integer, LocalDate> maxDatePerIdMap = customRepository.findAllFundDailyMaxDatePerId();

            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            ExecutorService exec = scrapeExecutor;
            try {
                List<Callable<Void>> tasks = new ArrayList<>();
                for (Integer fundId : fundIds) {
                    tasks.add(() -> {
                        try {
                            LocalDate startDate = maxDatePerIdMap
                                    .getOrDefault(fundId, LocalDate.of(2000, 1, 1))
                                    .plusDays(1);

                            log.debug("Scraping fund nav for fund id {} from {} to {}", fundId, startDate, endDate);
                            String fundNav = get(String.format(FUND_NAV_HISTORIC_URL, fundId, startDate, endDate));

                            List<FundDaily> fundDailies = objectMapper.readValue(fundNav, new TypeReference<>() {
                            });

                            if (fundDailies != null && !fundDailies.isEmpty()) {
                                log.debug("Inserting {} fund daily data for id {}", fundDailies.size(), fundId);
                                customRepository.insertAll(fundDailies);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch fund daily data for id {}", fundId, e);
                        }
                        return null;
                    });
                }

                List<Future<Void>> futures = exec.invokeAll(tasks);
                for (Future<Void> f : futures) {
                    try { f.get(); } catch (Exception ignored) {}
                }
            } finally {
                exec.shutdown();
            }
        } catch (Exception e) {
            log.error("Failed to scrape fund daily data");
        }
        logEndTime("fund daily data", startTime);
    }

    private void scrapeAllFundAumDaily(LocalDateTime startTime, Set<Integer> fundIdsParam) {
        log.info("Starting to scrape all fund aum daily");

        try {
            Set<Integer> fundIds = fundIdsParam;
            if (fundIds == null) {
                fundIds = customRepository.findAllFundIds();
            }
            log.info("Found {} funds to scrape for daily aum", fundIds.size());

            Map<Integer, LocalDate> maxDatePerIdMap = customRepository.findAllFundAumMaxDatePerId();

            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            ExecutorService exec = scrapeExecutor;
            try {
                List<Callable<Void>> tasks = new ArrayList<>();
                for (Integer fundId : fundIds) {
                    tasks.add(() -> {
                        try {
                            LocalDate startDate = maxDatePerIdMap
                                    .getOrDefault(fundId, LocalDate.of(2000, 1, 1))
                                    .plusDays(1);

                            log.debug("Scraping fund aum for fund id {} from {} to {}", fundId, startDate, endDate);
                            String fundAumRaw = get(String.format(FUND_AUM_HISTORIC_URL, fundId, startDate, endDate));

                            List<FundAum> fundAum = objectMapper.readValue(fundAumRaw, new TypeReference<>() {
                            });

                            if (fundAum != null && !fundAum.isEmpty()) {
                                log.debug("Inserting {} fund aum data for id {}", fundAum.size(), fundId);
                                customRepository.insertAll(fundAum);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch fund aum data for id {}", fundId, e);
                        }
                        return null;
                    });
                }

                List<Future<Void>> futures = exec.invokeAll(tasks);
                for (Future<Void> f : futures) {
                    try { f.get(); } catch (Exception ignored) {}
                }
            } finally {
                exec.shutdown();
            }
        } catch (Exception e) {
            log.error("Failed to scrape fund aum data");
        }
        logEndTime("fund aum data", startTime);
    }

    private void scrapeAllFundUnitDaily(LocalDateTime startTime, Set<Integer> fundIdsParam) {
        log.info("Starting to scrape all fund unit daily");

        try {
            Set<Integer> fundIds = fundIdsParam;
            if (fundIds == null) {
                fundIds = customRepository.findAllFundIds();
            }
            log.info("Found {} funds to scrape for daily unit", fundIds.size());

            List<CodeDate> maxDatePerId = customRepository.findAllFundUnitMaxDatePerId();
            Map<Integer, LocalDate> maxDatePerIdMap = maxDatePerId.stream()
                    .collect(Collectors.toMap(it -> Integer.valueOf(it.getCode()), CodeDate::getDate, (a, b) -> a));
            log.info("Found {} existing daily unit data", maxDatePerId.size());

            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            ExecutorService exec = scrapeExecutor;

    @PreDestroy
    public void shutdownExecutor() {
        scrapeExecutor.shutdown();
    }
            try {
                List<Callable<Void>> tasks = new ArrayList<>();
                for (Integer fundId : fundIds) {
                    tasks.add(() -> {
                        try {
                            LocalDate startDate = maxDatePerIdMap
                                    .getOrDefault(fundId, LocalDate.of(2000, 1, 1))
                                    .plusDays(1);

                            log.debug("Scraping fund unit for fund id {} from {} to {}", fundId, startDate, endDate);
                            String fundUnitRaw = get(String.format(FUND_UNIT_HISTORIC_URL, fundId, startDate, endDate));

                            List<FundUnit> fundUnit = objectMapper.readValue(fundUnitRaw, new TypeReference<>() {
                            });

                            if (fundUnit != null && !fundUnit.isEmpty()) {
                                log.debug("Inserting {} fund unit data for id {}", fundUnit.size(), fundId);
                                customRepository.insertAll(fundUnit);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch fund unit data for id {}", fundId, e);
                        }
                        return null;
                    });
                }

                List<Future<Void>> futures = exec.invokeAll(tasks);
                for (Future<Void> f : futures) {
                    try { f.get(); } catch (Exception ignored) {}
                }
            } finally {
                exec.shutdown();
            }
        } catch (Exception e) {
            log.error("Failed to scrape fund unit data");
        }
        logEndTime("fund unit data", startTime);
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
