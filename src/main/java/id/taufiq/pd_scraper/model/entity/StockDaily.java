package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Table("stock_daily")
public class StockDaily {
    @Id
    @JsonProperty("Id")
    private String id = UUID.randomUUID().toString();

    @JsonProperty("Code")
    private String code;

    @JsonProperty("OpeningPrice")
    private Integer openingPrice;

    @JsonProperty("ClosingPrice")
    private Integer closingPrice;

    @JsonProperty("HighPrice")
    private Integer highPrice;

    @JsonProperty("LowPrice")
    private Integer lowPrice;

    @JsonProperty("Volume")
    private Long volume;

    @JsonProperty("MarketCap")
    private Long marketCap;

    @JsonProperty("Date")
    private LocalDate date;

    private LocalDate createdAt;
}
