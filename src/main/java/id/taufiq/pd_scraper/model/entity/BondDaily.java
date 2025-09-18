package id.taufiq.pd_scraper.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
@Table("bond_daily")
public class BondDaily {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Id
    @JsonProperty("Id")
    private String id = UUID.randomUUID().toString();

    @Column("bond_code")
    @JsonProperty("BondCode")
    private String bondCode;

    @Column("bond_id")
    @JsonProperty("BondId")
    private Integer bondId;

    @Column("is_transacted")
    @JsonProperty("IsTransacted")
    private Boolean transacted;

    @Column("date")
    private LocalDate date;

    @Column("date_based")
    private LocalDateTime dateBased;

    @Column("high_price")
    @JsonProperty("HighPrice")
    private BigDecimal highPrice;

    @Column("low_price")
    @JsonProperty("LowPrice")
    private BigDecimal lowPrice;

    @Column("last_price")
    @JsonProperty("LastPrice")
    private BigDecimal lastPrice;

    @Column("wap")
    @JsonProperty("Wap")
    private BigDecimal wap;

    @Column("total_vol")
    @JsonProperty("TotalVol")
    private BigDecimal totalVol;

    @Column("total_val")
    @JsonProperty("TotalVal")
    private BigDecimal totalVal;

    @Column("freq")
    @JsonProperty("Freq")
    private BigDecimal freq;

    @Column("one_day_return")
    @JsonProperty("OneDayReturn")
    private BigDecimal oneDayReturn;

    @Column("one_week_return")
    @JsonProperty("OneWeekReturn")
    private BigDecimal oneWeekReturn;

    @Column("mtd_return")
    @JsonProperty("MtdReturn")
    private BigDecimal mtdReturn;

    @Column("one_month_return")
    @JsonProperty("OneMonthReturn")
    private BigDecimal oneMonthReturn;

    @Column("three_month_return")
    @JsonProperty("ThreeMonthReturn")
    private BigDecimal threeMonthReturn;

    @Column("six_month_return")
    @JsonProperty("SixMonthReturn")
    private BigDecimal sixMonthReturn;

    @Column("ytd_return")
    @JsonProperty("YtdReturn")
    private BigDecimal ytdReturn;

    @Column("one_year_return")
    @JsonProperty("OneYearReturn")
    private BigDecimal oneYearReturn;

    @Column("three_year_return")
    @JsonProperty("ThreeYearReturn")
    private BigDecimal threeYearReturn;

    @Column("five_year_return")
    @JsonProperty("FiveYearReturn")
    private BigDecimal fiveYearReturn;

    @Column("ten_year_return")
    @JsonProperty("TenYearReturn")
    private BigDecimal tenYearReturn;

    @Column("inception_return")
    @JsonProperty("InceptionReturn")
    private BigDecimal inceptionReturn;

    @Column("ttm")
    private BigDecimal ttm;

    @Column("ytm")
    private BigDecimal ytm;

    @Column("current_yield")
    private BigDecimal currentYield;

    @Column("modified_duration")
    private BigDecimal modifiedDuration;

    @Column("outstanding_amount")
    private BigDecimal outstandingAmount;

    @Column("additional_wap")
    private BigDecimal additionalWap;

    @JsonSetter("Date")
    public void setDateFromJson(String value) {
        this.date = parseDate(value);
    }

    @JsonSetter("DateBased")
    public void setDateBasedFromJson(String value) {
        this.dateBased = parseDateTime(value);
    }

    @JsonSetter("AdditionalData")
    public void setAdditionalData(AdditionalData additionalData) {
        if (additionalData == null) {
            return;
        }
        this.additionalWap = additionalData.getWap();
        this.ttm = additionalData.getTtm();
        this.ytm = additionalData.getYtm();
        this.currentYield = additionalData.getCurrentYield();
        this.modifiedDuration = additionalData.getModifiedDuration();
        this.outstandingAmount = additionalData.getOutstandingAmount();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDateTime(value).toLocalDate();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AdditionalData {
        @JsonProperty("Wap")
        private BigDecimal wap;
        @JsonProperty("Ttm")
        private BigDecimal ttm;
        @JsonProperty("Ytm")
        private BigDecimal ytm;
        @JsonProperty("CurrentYield")
        private BigDecimal currentYield;
        @JsonProperty("ModifiedDuration")
        private BigDecimal modifiedDuration;
        @JsonProperty("OutstandingAmount")
        private BigDecimal outstandingAmount;

        public BigDecimal getWap() {
            return wap;
        }

        public BigDecimal getTtm() {
            return ttm;
        }

        public BigDecimal getYtm() {
            return ytm;
        }

        public BigDecimal getCurrentYield() {
            return currentYield;
        }

        public BigDecimal getModifiedDuration() {
            return modifiedDuration;
        }

        public BigDecimal getOutstandingAmount() {
            return outstandingAmount;
        }
    }
}
