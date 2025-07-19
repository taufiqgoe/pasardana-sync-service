package id.taufiq.pd_scraper.repository;

import id.taufiq.pd_scraper.model.dao.CodeDate;
import id.taufiq.pd_scraper.model.dao.CodePeriod;
import id.taufiq.pd_scraper.model.dao.CodePeriodDate;
import id.taufiq.pd_scraper.model.dao.StockReportUpdate;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public <T> boolean existsById(Object id, Class<T> type) {
        return jdbcAggregateTemplate.existsById(id, type);
    }

    public Set<Integer> findAllExistingFundIds() {
        String query = "select distinct id from funds";
        return new HashSet<>(jdbcTemplate.queryForList(query, Integer.class));
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

    public Map<String, List<String>> findAllCurrentStockReportPeriod() {
        String query = "SELECT code, string_agg(DISTINCT \"period\", ',' ORDER BY \"period\") AS \"period\" FROM stock_reports GROUP BY code ORDER BY code";
        List<CodePeriod> currentPeriods = jdbcTemplate.queryForList(query, CodePeriod.class);

        return currentPeriods.stream()
                .collect(Collectors.toMap(
                        CodePeriod::getCode,
                        cp -> List.of(cp.getPeriod().split(",")),
                        (existing, replacement) -> existing
                ));
    }

    public void batchUpdateStockReports(List<StockReportUpdate> updates) {
        String query = """
                UPDATE public.stock_reports
                SET value = ?, last_update = ?
                WHERE code = ? AND "period" = ? AND property_id = ?
                """;

        jdbcTemplate.batchUpdate(query, updates, updates.size(),
                (ps, dto) -> {
                    ps.setBigDecimal(1, dto.getValue());
                    ps.setObject(2, dto.getLastUpdate());
                    ps.setString(3, dto.getCode());
                    ps.setString(4, dto.getPeriod());
                    ps.setInt(5, dto.getPropertyId());
                }
        );
    }
}
