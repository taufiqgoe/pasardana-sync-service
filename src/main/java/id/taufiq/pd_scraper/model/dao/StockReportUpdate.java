package id.taufiq.pd_scraper.model.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReportUpdate {
    private BigDecimal value;
    private LocalDateTime lastUpdate;
    private String code;
    private String period;
    private Integer propertyId;
}
