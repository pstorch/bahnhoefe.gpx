package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.railwaystations.api.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public interface StationDao {

    String JOIN_QUERY = "select s.countryCode, s.id, s.DS100, s.title, s.lat, s.lon, p.url, p.license, p.createdAt, u.name, u.url as photographerUrl, u.license as photographerLicense, u.anonymous from stations s left join photos p on p.countryCode = s.countryCode and p.id = s.id left join users u on u.id = p.photographerId";

    @SqlQuery(JOIN_QUERY + " where s.countryCode in (<countryCodes>)")
    @RegisterRowMapper(StationMapper.class)
    Set<Station> findByCountryCodes(@BindList("countryCodes") final Set<String> countryCodes);

    @SqlQuery(JOIN_QUERY)
    @RegisterRowMapper(StationMapper.class)
    Set<Station> all();

    @SqlQuery(JOIN_QUERY + " where (s.countryCode = :countryCode or :countryCode is null) and s.id = :id")
    @RegisterRowMapper(StationMapper.class)
    Set<Station> findByKey(@Bind("countryCode") final String countryCode, @Bind("id") final String id);

    @SqlQuery("select count(*) stations, count(p.url) photos, count(distinct p.photographerId) photographers from stations s left join photos p on p.countryCode = s.countryCode and p.id = s.id where s.countryCode = :countryCode or :countryCode is null")
    @RegisterRowMapper(StatisticMapper.class)
    @SingleValue
    Statistic getStatistic(@Bind("countryCode") final String countryCode);

    @SqlQuery("select u.name photographer, count(*) photocount from stations s join photos p on p.countryCode = s.countryCode and p.id = s.id join users u on u.id = p.photographerId where s.countryCode = :countryCode or :countryCode is null group by u.name order by count(*) desc")
    @KeyColumn("photographer")
    @ValueColumn("photocount")
    Map<String, Long> getPhotographerMap(@Bind("countryCode") final String countryCode);

    @SqlQuery("select countryCode s_country, id s_id, title from stations s where LOCATE(LOWER(:name), LOWER(title)) > 0")
    @RegisterConstructorMapper(value = Station.Key.class, prefix = "s")
    @ValueColumn("title")
    Map<Station.Key, String> findByName(@Bind("name") final String name);

    class StationMapper implements RowMapper<Station> {

        private static String photoBaseUrl = "";

        public static void setPhotoBaseUrl(final String url) {
            photoBaseUrl = url;
        }

        public Station map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            final Station.Key key = new Station.Key(rs.getString("countryCode"), rs.getString("id"));
            final String photoUrl = rs.getString("url");
            Photo photo = null;
            if (photoUrl != null) {
                final User photographer = new User(rs.getString("name"), rs.getString("photographerUrl"), rs.getString("photographerLicense"), rs.getBoolean("anonymous"));
                photo = new Photo(key, photoBaseUrl + photoUrl, photographer, rs.getLong("createdAt"), rs.getString("license"));
            }
            return new Station(key, rs.getString("title"),
                    new Coordinates(rs.getDouble("lat"), rs.getDouble("lon")),
                    rs.getString("DS100"), photo);
        }

    }

    class StatisticMapper implements RowMapper<Statistic> {
        @Override
        public Statistic map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Statistic(rs.getInt("stations"), rs.getInt("photos"), rs.getInt("photographers"));
        }
    }
}
