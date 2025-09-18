package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("bonds")
public class Bond {
    @Id
    @JsonProperty("Code")
    private String code;

    @Column("isin_code")
    @JsonProperty("IsinCode")
    private String isinCode;

    @Column("name")
    @JsonProperty("Name")
    private String name;

    @Column("type")
    @JsonProperty("Type")
    private String type;

    @Column("bond_id")
    @JsonProperty("BondId")
    private Integer bondId;

    @Column("interest_rate")
    @JsonProperty("InterestRate")
    private Double interestRate;

    @Column("interest_type")
    @JsonProperty("InterestType")
    private String interestType;

    @Column("interest_frequency_code")
    @JsonProperty("InterestFrequencyCode")
    private String interestFrequencyCode;

    @Column("interest_frequency")
    @JsonProperty("InterestFrequency")
    private String interestFrequency;

    @Column("issue_date")
    @JsonProperty("IssueDate")
    private LocalDateTime issueDate;

    @Column("listing_date")
    @JsonProperty("ListingDate")
    private LocalDateTime listingDate;

    @Column("mature_date")
    @JsonProperty("MatureDate")
    private LocalDateTime matureDate;

    @Column("sharia")
    @JsonProperty("Sharia")
    private Boolean sharia;
}
