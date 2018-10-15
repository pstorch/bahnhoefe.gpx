package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.railwaystations.api.model.Country;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public interface CountryDao {

    @SqlQuery("select * from countries where id = :id")
    @RegisterRowMapper(CountryMapper.class)
    Optional<Country> findById(@Bind("id") final String id);

    @SqlQuery("select * from countries")
    @RegisterRowMapper(CountryMapper.class)
    Set<Country> list();

    class CountryMapper implements RowMapper<Country> {
        public CountryMapper() {
        }

        public Country map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new Country(rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("twitterTags"),
                    rs.getString("timetableUrlTemplate"),
                    rs.getString("stationsIndex"),
                    rs.getString("photosIndex")
                    );
        }

    }

}
