package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("stocks")
public class Stock {
    @Id
    @JsonProperty("Code")
    private String code;
    @JsonProperty("Name")
    private String name;
}
