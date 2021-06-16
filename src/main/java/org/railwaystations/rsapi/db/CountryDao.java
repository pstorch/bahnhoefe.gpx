package org.railwaystations.rsapi.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.railwaystations.rsapi.model.Country;
import org.railwaystations.rsapi.model.ProviderApp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CountryDao {

    @SqlQuery("select " +
            "c.id c_id, c.name c_name, c.email c_email, c.twitterTags c_twitterTags, c.timetableUrlTemplate c_timetableUrlTemplate, c.overrideLicense c_overrideLicense, c.active c_active, " +
            "p.type p_type, p.name p_name, p.url p_url " +
            "from countries c left join providerApps p " +
            "on c.id = p.countryCode " +
            "where c.id = :id")
    @UseRowReducer(CountryProviderAppReducer.class)
    @RegisterRowMapper(CountryMapper.class)
    @RegisterRowMapper(ProviderAppMapper.class)
    Optional<Country> findById(@Bind("id") final String id);

    @SqlQuery("select " +
            "c.id c_id, c.name c_name, c.email c_email, c.twitterTags c_twitterTags, c.timetableUrlTemplate c_timetableUrlTemplate, c.overrideLicense c_overrideLicense, c.active c_active, " +
            "p.type p_type, p.name p_name, p.url p_url " +
            "from countries c left join providerApps p " +
            "on c.id = p.countryCode " +
            "where :onlyActive = false or c.active = true")
    @UseRowReducer(CountryProviderAppReducer.class)
    @RegisterRowMapper(CountryMapper.class)
    @RegisterRowMapper(ProviderAppMapper.class)
    Set<Country> list(@Bind("onlyActive")  final boolean onlyActive);

    class CountryMapper implements RowMapper<Country> {
        public Country map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new Country(rs.getString("c_id"),
                    rs.getString("c_name"),
                    rs.getString("c_email"),
                    rs.getString("c_twitterTags"),
                    rs.getString("c_timetableUrlTemplate"),
                    rs.getString("c_overrideLicense"),
                    rs.getBoolean("c_active")
                    );
        }
    }

    class ProviderAppMapper implements RowMapper<ProviderApp> {
        public ProviderApp map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new ProviderApp(rs.getString("p_type"),
                    rs.getString("p_name"),
                    rs.getString("p_url")
            );
        }
    }

    class CountryProviderAppReducer implements LinkedHashMapRowReducer<String, Country> {
        @Override
        public void accumulate(final Map<String, Country> map, final RowView rowView) {
            final Country c = map.computeIfAbsent(rowView.getColumn("c_id", String.class),
                    id -> rowView.getRow(Country.class));

            if (rowView.getColumn("p_type", String.class) != null) {
                c.getProviderApps().add(rowView.getRow(ProviderApp.class));
            }
        }
    }

}
