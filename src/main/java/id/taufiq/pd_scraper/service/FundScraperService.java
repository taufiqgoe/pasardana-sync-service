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

@Slf4j
@Service
public class FundScraperService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CustomRepository customRepository;

    public FundScraperService(ObjectMapper objectMapper, RestClient restClient, CustomRepository customRepository) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.customRepository = customRepository;
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAll() {
        LocalDateTime startTime = LocalDateTime.now();
        scrapeFunds();
        scrapeAllFundNavDaily(startTime);
        scrapeAllFundAumDaily(startTime);
        scrapeAllFundUnitDaily(startTime);
    }

    private void scrapeFunds() {
        log.info("Starting to scrape funds");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String getAll = get("https://pasardana.id/api/FundAPI/SearchFund");
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

    private void scrapeAllFundNavDaily(LocalDateTime startTime) {
        log.info("Starting to scrape all fund nav daily");

        try {
            List<Integer> fundIds = customRepository.findAll(Fund.class).stream().map(Fund::getId).toList();
            log.info("Found {} funds to scrape for daily nav", fundIds.size());

            Map<Integer, LocalDate> maxDatePerIdMap = customRepository.findAllFundDailyMaxDatePerId();

            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            fundIds.parallelStream().forEach(fundId -> {
                LocalDate startDate = maxDatePerIdMap
                        .getOrDefault(fundId, LocalDate.of(2000, 1, 1))
                        .plusDays(1);

                try {
                    log.debug("Scraping fund nav for fund id {} from {} to {}", fundId, startDate, endDate);
                    String fundNav = get("https://pasardana.id/api/FundAPI/GetFundNAVHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s"
                            .formatted(fundId, startDate, endDate));

                    List<FundDaily> fundDailies = objectMapper.readValue(fundNav, new TypeReference<>() {
                    });

                    if (fundDailies != null && !fundDailies.isEmpty()) {
                        log.debug("Inserting {} fund daily data for id {}", fundDailies.size(), fundId);
                        customRepository.insertAll(fundDailies);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch fund daily data for id {}", fundId, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape fund daily data");
        }
        logEndTime("fund daily data", startTime);
    }

    private void scrapeAllFundAumDaily(LocalDateTime startTime) {
        log.info("Starting to scrape all fund aum daily");

        try {
            List<Integer> fundIds = customRepository.findAll(Fund.class).stream().map(Fund::getId).toList();
            log.info("Found {} funds to scrape for daily aum", fundIds.size());

            Map<Integer, LocalDate> maxDatePerIdMap = customRepository.findAllFundAumMaxDatePerId();

            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            fundIds.parallelStream().forEach(fundId -> {
                try {
                    LocalDate startDate = maxDatePerIdMap
                            .getOrDefault(fundId, LocalDate.of(2000, 1, 1))
                            .plusDays(1);

                    log.debug("Scraping fund aum for fund id {} from {} to {}", fundId, startDate, endDate);
                    String fundAumRaw = get("https://pasardana.id/api/FundAPI/GetFundAUMHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s"
                            .formatted(fundId, startDate, endDate));

                    List<FundAum> fundAum = objectMapper.readValue(fundAumRaw, new TypeReference<>() {
                    });

                    if (fundAum != null && !fundAum.isEmpty()) {
                        log.debug("Inserting {} fund aum data for id {}", fundAum.size(), fundId);
                        customRepository.insertAll(fundAum);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch fund aum data for id {}", fundId, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape fund aum data");
        }
        logEndTime("fund aum data", startTime);
    }

    private void scrapeAllFundUnitDaily(LocalDateTime startTime) {
        log.info("Starting to scrape all fund unit daily");

        try {
            List<Integer> fundIds = customRepository.findAll(Fund.class).stream().map(Fund::getId).toList();
            log.info("Found {} funds to scrape for daily unit", fundIds.size());

            List<CodeDate> maxDatePerId = customRepository.findAllFundUnitMaxDatePerId();
            Map<Integer, LocalDate> maxDatePerIdMap = maxDatePerId.stream()
                    .collect(Collectors.toMap(it -> Integer.valueOf(it.getCode()), CodeDate::getDate, (a, b) -> a));
            log.info("Found {} existing daily unit data", maxDatePerId.size());

            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            fundIds.parallelStream().forEach(fundId -> {
                try {
                    LocalDate startDate = maxDatePerIdMap
                            .getOrDefault(fundId, LocalDate.of(2000, 1, 1))
                            .plusDays(1);

                    log.debug("Scraping fund unit for fund id {} from {} to {}", fundId, startDate, endDate);
                    String fundUnitRaw = get("https://pasardana.id/api/FundAPI/GetFundUPHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s"
                            .formatted(fundId, startDate, endDate));

                    List<FundUnit> fundUnit = objectMapper.readValue(fundUnitRaw, new TypeReference<>() {
                    });

                    if (fundUnit != null && !fundUnit.isEmpty()) {
                        log.debug("Inserting {} fund unit data for id {}", fundUnit.size(), fundId);
                        customRepository.insertAll(fundUnit);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch fund unit data for id {}", fundId, e);
                }
            });
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
