package id.taufiq.pd_scraper.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.taufiq.pd_scraper.model.entity.Bond;
import id.taufiq.pd_scraper.model.entity.BondDaily;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class BondScraperService {

    private static final String BOND_PROFILE_URL = "https://pasardana.id/api/BondAPI/GetBondProfile?all=all";
    private static final String BOND_ID_URL = "https://pasardana.id/api/bondAPI/GetBondsId";
    private static final String BOND_DATA_ADDITION_URL = "https://pasardana.id/api/BondAPI/GetBondDataAddition?code=%s&datestart=%s&dateend=%s&complete=complete";
    private static final LocalDate DEFAULT_BOND_DAILY_START_DATE = LocalDate.of(2000, 1, 1);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final CustomRepository customRepository;

    public BondScraperService(ObjectMapper objectMapper, RestClient restClient, CustomRepository customRepository) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.customRepository = customRepository;

        scrapeAll();
    }

    @Scheduled(cron = "#{@appProperties.syncCron}")
    private void scrapeAll() {
        LocalDateTime startTime = LocalDateTime.now();
        Map<String, Integer> bondIdsByCode = fetchBondIdsByCode();
        scrapeBonds(bondIdsByCode);
        scrapeBondDaily(startTime, bondIdsByCode);
    }

    private void scrapeBonds(Map<String, Integer> bondIdsByCode) {
        log.info("Starting to scrape bonds");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            String rawBonds = get(BOND_PROFILE_URL);
            List<Bond> bonds = objectMapper.readValue(rawBonds, new TypeReference<>() {
            });

            log.info("Found {} bonds", bonds.size());

            Set<String> existingBondCodes = customRepository.findAllExistingBondCodes();
            log.info("Found {} existing bonds", existingBondCodes.size());

            List<Bond> bondsToUpdate = new ArrayList<>();
            List<Bond> bondsToInsert = new ArrayList<>();

            for (Bond bond : bonds) {
                String code = bond.getCode();
                if (code == null || code.isBlank()) {
                    log.debug("Skipping bond without code");
                    continue;
                }

                Integer bondId = bondIdsByCode.get(code);
                if (bondId == null) {
                    log.debug("No bond id found for code {}", code);
                } else {
                    bond.setBondId(bondId);
                }

                if (existingBondCodes.contains(code)) {
                    log.debug("Updating bond {}", code);
                    bondsToUpdate.add(bond);
                } else {
                    log.debug("Inserting bond {}", code);
                    bondsToInsert.add(bond);
                }
            }

            if (!bondsToUpdate.isEmpty()) {
                customRepository.updateAll(bondsToUpdate);
            }
            if (!bondsToInsert.isEmpty()) {
                customRepository.insertAll(bondsToInsert);
            }

        } catch (Exception e) {
            log.error("Failed to scrape bonds", e);
        }

        logEndTime("bonds", startTime);
    }

    private void scrapeBondDaily(LocalDateTime startTime, Map<String, Integer> bondIdsByCode) {
        log.info("Starting to scrape bond daily");

        try {
            Set<String> bondCodes = customRepository.findAllExistingBondCodes();
            if (bondCodes.isEmpty()) {
                log.info("No bond codes found to scrape bond daily data");
                return;
            }

            Map<String, LocalDate> maxDatePerCodeMap = customRepository.findAllBondDailyMaxDatePerCode();
            LocalDate endDate = startTime.toLocalDate().plusDays(1);

            bondCodes.parallelStream().forEach(code -> {
                try {
                    LocalDate maxDate = maxDatePerCodeMap.get(code);
                    LocalDate startDate = maxDate != null ? maxDate.plusDays(1) : DEFAULT_BOND_DAILY_START_DATE;

                    if (startDate.isAfter(endDate)) {
                        return;
                    }

                    String endpoint = String.format(BOND_DATA_ADDITION_URL, code, startDate, endDate);
                    String bondDailyRaw = get(endpoint);
                    List<BondDaily> bondDailies = objectMapper.readValue(bondDailyRaw, new TypeReference<List<BondDaily>>() {
                    });

                    if (bondDailies == null || bondDailies.isEmpty()) {
                        return;
                    }

                    bondDailies.forEach(daily -> {
                        if (daily.getBondCode() == null) {
                            daily.setBondCode(code);
                        }
                        if (daily.getBondId() == null && bondIdsByCode.containsKey(code)) {
                            daily.setBondId(bondIdsByCode.get(code));
                        }
                    });

                    Map<String, BondDaily> uniqueDaily = bondDailies.stream()
                            .filter(it -> it.getBondCode() != null && it.getDate() != null)
                            .collect(toMap(
                                    it -> it.getBondCode() + "|" + it.getDate(),
                                    Function.identity(),
                                    (existing, replacement) -> existing
                            ));

                    List<BondDaily> uniqueBondDailies = new ArrayList<>(uniqueDaily.values());
                    if (!uniqueBondDailies.isEmpty()) {
                        customRepository.insertAll(uniqueBondDailies);
                        log.debug("Inserted {} bond daily records for code {}", uniqueBondDailies.size(), code);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch bond daily for code {}", code, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to scrape bond daily", e);
        }

        logEndTime("bond daily", startTime);
    }

    private Map<String, Integer> fetchBondIdsByCode() {
        try {
            String rawBondIds = get(BOND_ID_URL);
            List<BondIdResponse> bondIdResponses = objectMapper.readValue(rawBondIds, new TypeReference<>() {
            });

            Map<String, Integer> result = new HashMap<>();
            for (BondIdResponse response : bondIdResponses) {
                if (response == null) {
                    continue;
                }

                String code = response.getBondCode();
                Integer bondId = response.getBondId();

                if (code == null || code.isBlank() || bondId == null) {
                    continue;
                }

                result.putIfAbsent(code, bondId);
            }

            log.info("Fetched {} bond ids", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch bond ids", e);
            return Collections.emptyMap();
        }
    }

    private String get(String endpoint) {
        ResponseEntity<byte[]> entity = restClient.get()
                .uri(endpoint)
                .retrieve()
                .toEntity(byte[].class);

        if (entity.getBody() == null) {
            throw new RuntimeException("Empty response body");
        }

        return new String(entity.getBody(), StandardCharsets.UTF_8);
    }

    private void logEndTime(String event, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        log.info("Finished scraping {} with time spent: {} minutes {} seconds", event, minutes, seconds);
    }

    private static class BondIdResponse {
        @JsonProperty("BondId")
        private Integer bondId;
        @JsonProperty("BondCode")
        private String bondCode;

        Integer getBondId() {
            return bondId;
        }

        String getBondCode() {
            return bondCode;
        }
    }
}
