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
import java.util.Comparator;
import java.util.List;

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

    @Scheduled(cron = "#{@appProperties.baseProductSyncCron}")
    private void scrapeFunds() {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String getAll = get("https://pasardana.id/api/FundAPI/SearchFund");
            List<Fund> funds = objectMapper.readValue(getAll, new TypeReference<List<Fund>>() {
                    }).stream()
                    .filter(it -> it.getId() != null && it.getId() > 0)
                    .sorted(Comparator.comparing(Fund::getId))
                    .toList();

            funds.parallelStream().forEach(fund -> {
                try {
                    boolean exists = customRepository.existsById(fund.getId(), Fund.class);
                    if (exists) {
                        customRepository.update(fund);
                    } else {
                        customRepository.insert(fund);
                    }
                } catch (Exception e) {
                    log.error("Failed to upsert fund for id {}", fund.getId(), e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape funds", e);
        }
        logEndTime("funds", startTime);
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAllFundNavDaily() {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<Integer> fundIds = customRepository.findAll(Fund.class).stream().map(Fund::getId).toList();

            List<CodeDate> maxDatePerId = customRepository.findAllFundDailyMaxDatePerId();

            LocalDate endDate = LocalDate.now().plusDays(1);

            fundIds.parallelStream().forEach(fundId -> {
                LocalDate startDate = maxDatePerId.stream()
                        .filter(it -> it.getCode().equalsIgnoreCase(fundId.toString()))
                        .findFirst()
                        .map(CodeDate::getDate)
                        .orElse(LocalDate.of(2000, 1, 1))
                        .plusDays(1);

                try {
                    String fundNav = get("https://pasardana.id/api/FundAPI/GetFundNAVHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s"
                            .formatted(fundId, startDate, endDate));

                    List<FundDaily> fundDailies = objectMapper.readValue(fundNav, new TypeReference<>() {
                    });

                    if (fundDailies != null && !fundDailies.isEmpty()) {
                        log.debug("Inserting {} fund daily data for id {}", fundDailies.size(), fundId);
                        customRepository.insertAll(fundDailies);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch fund daily data for id {}", fundId, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape fund daily data");
        }
        logEndTime("fund daily data", startTime);
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAllFundAumDaily() {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<Integer> fundIds = customRepository.findAll(Fund.class).stream().map(Fund::getId).toList();

            List<CodeDate> maxDatePerId = customRepository.findAllFundNavMaxDatePerId();

            LocalDate endDate = LocalDate.now().plusDays(1);

            fundIds.parallelStream().forEach(fundId -> {
                try {
                    LocalDate startDate = maxDatePerId.stream()
                            .filter(it -> it.getCode().equalsIgnoreCase(fundId.toString()))
                            .findFirst()
                            .map(CodeDate::getDate)
                            .orElse(LocalDate.of(2000, 1, 1))
                            .plusDays(1);

                    String fundAumRaw = get("https://pasardana.id/api/FundAPI/GetFundAUMHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s"
                            .formatted(fundId, startDate, endDate));

                    List<FundAum> fundAum = objectMapper.readValue(fundAumRaw, new TypeReference<>() {
                    });

                    if (fundAum != null && !fundAum.isEmpty()) {
                        log.debug("Inserting {} fund aum data for id {}", fundAum.size(), fundId);
                        customRepository.insertAll(fundAum);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch fund aum data for id {}", fundId, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape fund aum data");
        }
        logEndTime("fund aum data", startTime);
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAllFundUnitDaily() {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<Integer> fundIds = customRepository.findAll(Fund.class).stream().map(Fund::getId).toList();

            List<CodeDate> maxDatePerId = customRepository.findAllFundUnitMaxDatePerId();

            LocalDate endDate = LocalDate.now().plusDays(1);

            fundIds.parallelStream().forEach(fundId -> {
                try {
                    LocalDate startDate = maxDatePerId.stream()
                            .filter(it -> it.getCode().equalsIgnoreCase(fundId.toString()))
                            .findFirst()
                            .map(CodeDate::getDate)
                            .orElse(LocalDate.of(2000, 1, 1))
                            .plusDays(1);

                    String fundUnitRaw = get("https://pasardana.id/api/FundAPI/GetFundUPHistoricData?fundId=%s&dateBegin=%s&dateEnd=%s"
                            .formatted(fundId, startDate, endDate));

                    List<FundUnit> fundUnit = objectMapper.readValue(fundUnitRaw, new TypeReference<>() {
                    });

                    if (fundUnit != null && !fundUnit.isEmpty()) {
                        log.debug("Inserting {} fund unit data for id {}", fundUnit.size(), fundId);
                        customRepository.insertAll(fundUnit);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch fund unit data for id {}", fundId, e);
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
