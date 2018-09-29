package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.railwaystations.api.model.Country;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface CountryDao {

    @SqlQuery("select * from countries")
    @RegisterRowMapper(CountryMapper.class)
    List<Country> list();

    class CountryMapper implements RowMapper<Country> {
        public CountryMapper() {
        }

        public Country map(ResultSet rs, StatementContext ctx) throws SQLException {
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
