package id.taufiq.pd_scraper.repository;

import id.taufiq.pd_scraper.model.dao.CodeDate;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Repository
public class CustomRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    public CustomRepository(JdbcTemplate jdbcTemplate, JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    public <T> void insert(T entity) {
        jdbcAggregateTemplate.insert(entity);
    }

    public <T> void update(T entity) {
        jdbcAggregateTemplate.update(entity);
    }

    public <T> void insertAll(List<T> entities) {
        jdbcAggregateTemplate.insertAll(entities);
    }

    public <T> void updateAll(List<T> entities) {
        jdbcAggregateTemplate.updateAll(entities);
    }

    public <T> List<T> findAll(Class<T> type) {
        return jdbcAggregateTemplate.findAll(type);
    }

    public Set<Integer> findAllExistingFundIds() {
        String query = "select distinct id from funds";
        return new HashSet<>(jdbcTemplate.queryForList(query, Integer.class));
    }

    public Set<String> findAllExistingStockCodes() {
        String query = "select distinct code from stocks";
        return new HashSet<>(jdbcTemplate.queryForList(query, String.class));
    }

    public List<Integer> findAllFundIds() {
        String query = "select id from funds";
        return jdbcTemplate.queryForList(query, Integer.class);
    }

    public List<String> findAllStockCodes() {
        String query = "select code from stocks";
        return jdbcTemplate.queryForList(query, String.class);
    }

    public Map<String, LocalDate> findAllStockDailyMaxDatePerCode() {
        String query = "select code, max(\"date\") as date from stock_daily group by code";
        List<CodeDate> maxDatePerCode = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
        return maxDatePerCode.stream().collect(toMap(CodeDate::getCode, CodeDate::getDate));
    }

    public Map<Integer, LocalDate> findAllFundDailyMaxDatePerId() {
        String query = "select fund_id as code, max(\"date\") as \"date\" from fund_daily group by fund_id";
        List<CodeDate> maxDatePerId = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
        return maxDatePerId.stream()
                .collect(Collectors.toMap(it -> Integer.valueOf(it.getCode()), CodeDate::getDate, (a, b) -> a));
    }

    public Map<Integer, LocalDate> findAllFundAumMaxDatePerId() {
        String query = "select fund_id as code, max(\"date\") as \"date\" from fund_aum group by fund_id";
        List<CodeDate> maxDatePerId = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
        return maxDatePerId.stream()
                .collect(Collectors.toMap(it -> Integer.valueOf(it.getCode()), CodeDate::getDate, (a, b) -> a));
    }

    public List<CodeDate> findAllFundUnitMaxDatePerId() {
        String query = "select fund_id as code, max(\"date\") as \"date\" from fund_unit group by fund_id";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
    }
}
