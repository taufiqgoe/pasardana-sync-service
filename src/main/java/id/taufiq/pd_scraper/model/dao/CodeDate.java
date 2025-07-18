package id.taufiq.pd_scraper.model.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeDate {
    private String code;
    private LocalDate date;
}
