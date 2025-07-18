package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("fund_daily")
public class FundDaily {
    @Id
    @JsonProperty("Id")
    private String id = UUID.randomUUID().toString();
    @JsonProperty("FundId")
    private Short fundId;
    @JsonProperty("Value")
    private BigDecimal value;
    @JsonProperty("DailyReturn")
    private BigDecimal dailyReturn;
    @JsonProperty("Date")
    private LocalDate date;
}
