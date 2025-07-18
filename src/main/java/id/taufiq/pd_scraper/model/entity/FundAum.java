package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("fund_aum")
public class FundAum {
    @Id
    private String id = UUID.randomUUID().toString();
    @JsonProperty("FundId")
    private Integer fundId;
    @JsonProperty("Value")
    private Long value;
    @JsonProperty("Date")
    private LocalDate date;
}
