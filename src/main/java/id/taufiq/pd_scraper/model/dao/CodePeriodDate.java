package id.taufiq.pd_scraper.model.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodePeriodDate {
    private String code;
    private String period;
    private LocalDate date;
}
