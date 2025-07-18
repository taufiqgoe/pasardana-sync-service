package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("funds")
public class Fund {
    @Id
    @JsonProperty("Id")
    private Integer id;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Type")
    private Integer type;
    @JsonProperty("IsActive")
    private Boolean active;
    @JsonProperty("Sharia")
    private Boolean sharia;
}
