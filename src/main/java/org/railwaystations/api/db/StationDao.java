package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.railwaystations.api.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public interface StationDao {

    // TODO: join user
    @SqlQuery("select s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, p.url, p.license, p.createdAt from stations s left join photos p on p.countryCode = s.countryCode and p.id = s.id left join users u on u.id = p.photographerId where id = :id")
    @RegisterRowMapper(StationMapper.class)
    Optional<Station> findById(@Bind("id") final String id);

    // TODO: join like above
    @SqlQuery("select * from stations")
    @RegisterRowMapper(StationMapper.class)
    Set<Station> list();

    class StationMapper implements RowMapper<Station> {
        public StationMapper() {
        }

        public Station map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            final Station.Key key = new Station.Key(rs.getString("countryCode"), rs.getString("id"));
            final String photoUrl = rs.getString("url");
            Photo photo = null;
            if (photoUrl != null) {
                User photographer = null; // TODO: read user from rs
                photo = new Photo(key, photoUrl, photographer, rs.getLong("createdAt"), rs.getString("license"));
            }
            return new Station(key, rs.getString("title"),
                    new Coordinates(rs.getDouble("lat"), rs.getDouble("lon")),
                    rs.getString("DS100"), photo);
        }

    }

}
