package id.taufiq.pd_scraper.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("stock_reports")
@NoArgsConstructor
@AllArgsConstructor
public class StockReport {
    @Id
    private String id;
    private String code;
    private String period;
    private Integer propertyId;
    private BigDecimal value;
    private LocalDateTime lastUpdate;
}
