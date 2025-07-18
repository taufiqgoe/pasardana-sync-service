package id.taufiq.pd_scraper.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetStockReport {
    @JsonProperty("Code")
    private String code;

    @JsonProperty("Period")
    private String period;

    @JsonProperty("LastUpdate")
    private LocalDateTime lastUpdate;

    @JsonProperty("Details")
    private List<Detail> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        @JsonProperty("PropertyId")
        private Integer propertyId;

        @JsonProperty("Value")
        private BigDecimal value;
    }
}
