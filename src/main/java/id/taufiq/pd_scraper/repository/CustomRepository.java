package id.taufiq.pd_scraper.repository;

import id.taufiq.pd_scraper.model.dao.CodeDate;
import id.taufiq.pd_scraper.model.dao.CodePeriodDate;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    public <T> List<T> findAll(Class<T> type) {
        return jdbcAggregateTemplate.findAll(type);
    }

    public <T> boolean existsById(Object id, Class<T> type) {
        return jdbcAggregateTemplate.existsById(id, type);
    }

    public List<CodeDate> findAllStockDailyMaxDatePerCode() {
        String query = "select code, max(\"date\") as date from stock_daily group by code";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
    }

    public List<CodeDate> findAllFundDailyMaxDatePerId() {
        String query = "select fund_id as code, max(\"date\") as \"date\" from fund_daily group by fund_id";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
    }

    public List<CodeDate> findAllFundNavMaxDatePerId() {
        String query = "select fund_id as code, max(\"date\") as \"date\" from fund_aum group by fund_id";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
    }

    public List<CodePeriodDate> findAllStockReportMaxUpdateDatePerCode() {
        String query = "SELECT code, \"period\", MAX(last_update::date) as \"date\" FROM stock_reports GROUP BY code, \"period\"";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodePeriodDate.class));
    }

    public List<CodeDate> findAllFundUnitMaxDatePerId() {
        String query = "select fund_id as code, max(\"date\") as \"date\" from fund_unit group by fund_id";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CodeDate.class));
    }

    public List<String> findAllCurrentStockReportPeriod(String code) {
        String query = "select distinct \"period\" from stock_reports where code = ? order by \"period\"";
        return jdbcTemplate.queryForList(query, String.class, code);
    }

    public void updateStockReportValueAndLastUpdateByCodeAndPeriodAndPropertyId(BigDecimal value, LocalDateTime lastUpdate, String code, String period, Integer propertyId) {
        String query = """
                UPDATE public.stock_reports
                SET
                  value = ?,
                  last_update = ?
                WHERE
                  code = ?
                  AND "period" = ?
                  AND property_id = ?
                """;
        jdbcTemplate.update(query, value, lastUpdate, code, period, propertyId);
    }
}
