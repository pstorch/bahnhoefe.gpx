package org.railwaystations.rsapi.db;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.model.Coordinates;
import org.railwaystations.rsapi.model.InboxEntry;
import org.railwaystations.rsapi.model.ProblemReportType;
import org.railwaystations.rsapi.model.PublicInboxEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface InboxDao {

    String JOIN_QUERY = "select u.id, u.countryCode, u.stationId, u.title u_title, s.title s_title, u.lat u_lat, u.lon u_lon, s.lat s_lat, s.lon s_lon, "
                    + "     u.photographerId, p.name photographerNickname, p.email photographerEmail, u.extension, u.comment, u.rejectReason, u.createdAt, "
                    + "     u.done, u.problemReportType, u.active, u.crc32, u.notified, f.url, "
                    + "     (select count(*) from inbox u2 where u2.countryCode is not null and u2.countryCode = u.countryCode "
                    + "         and u2.stationId is not null and u2.stationId = u.stationId and u2.done = false and u2.id != u.id) as conflict"
                    + " from inbox u left join stations s on s.countryCode = u.countryCode and s.id = u.stationId "
                    + "     left join users p on p.id = u.photographerId "
                    + "     left join photos f on f.countryCode = u.countryCode and f.id = u.stationId";

    @SqlQuery(JOIN_QUERY + " where u.id = :id")
    @RegisterRowMapper(InboxEntryMapper.class)
    InboxEntry findById(@Bind("id") final int id);

    @SqlQuery(JOIN_QUERY + " where u.done = false order by id")
    @RegisterRowMapper(InboxEntryMapper.class)
    List<InboxEntry> findPendingInboxEntries();

    @SqlQuery("select u.countryCode, u.stationId, u.title u_title, s.title s_title, u.lat u_lat, u.lon u_lon, s.lat s_lat, s.lon s_lon" +
              " from inbox u left join stations s on s.countryCode = u.countryCode and s.id = u.stationId" +
              " where u.done = false and (u.problemReportType is null or u.problemReportType = '')")
    @RegisterRowMapper(PublicInboxEntryMapper.class)
    List<PublicInboxEntry> findPublicInboxEntries();

    @SqlUpdate("insert into inbox (countryCode, stationId, title, lat, lon, photographerId, extension, comment, done, createdAt, problemReportType, active) values (:countryCode, :stationId, :title, :coordinates?.lat, :coordinates?.lon, :photographerId, :extension, :comment, :done, :createdAt, :problemReportType, :active)")
    @GetGeneratedKeys("id")
    Integer insert(@BindBean final InboxEntry inboxEntry);

    @SqlUpdate("update inbox set rejectReason = :rejectReason, done = true where id = :id")
    void reject(@Bind("id") final int id, @Bind("rejectReason") final String rejectReason);

    @SqlUpdate("update inbox set done = true where id = :id")
    void done(@Bind("id") int id);

    @SqlQuery("select count(*) from inbox where countryCode = :countryCode and stationId = :stationId and done = false and (:id is null or id <> :id)")
    int countPendingInboxEntriesForStation(@Bind("id") final Integer id, @Bind("countryCode") final String countryCode, @Bind("stationId") final String stationId);

    @SqlQuery("select count(*) from inbox where done = false")
    int countPendingInboxEntries();

    /**
     * Count nearby pending uploads using simple pythagoras (only valid for a few km)
     */
    @SqlQuery("select count(*) from inbox where sqrt(power(71.5 * (lon - :coords.lon),2) + power(111.3 * (lat - :coords.lat),2)) < 0.5 and done = false and (:id is null or id <> :id)")
    int countPendingInboxEntriesForNearbyCoordinates(@Bind("id") final Integer id, @BindBean("coords") final Coordinates coordinates);

    @SqlUpdate("update inbox set crc32 = :crc32 where id = :id")
    void updateCrc32(@Bind("id") Integer id, @Bind("crc32") Long crc32);

    @SqlQuery(JOIN_QUERY + " where u.countryCode = :countryCode and u.stationId = stationId and u.photographerId = :photographerId and done = false order by id desc")
    @RegisterRowMapper(InboxEntryMapper.class)
    InboxEntry findNewestPendingByCountryAndStationIdAndPhotographerId(@Bind("countryCode") String countryCode, @Bind("stationId") String stationId, @Bind("photographerId") int photographerId);

    @SqlQuery(JOIN_QUERY + " where u.done = true and u.notified = false")
    @RegisterRowMapper(InboxEntryMapper.class)
    List<InboxEntry> findInboxEntriesToNotify();

    @SqlUpdate("update inbox set notified = true where id in (<ids>)")
    void updateNotified(@BindList("ids") final List<Integer> ids);

    class InboxEntryMapper implements RowMapper<InboxEntry> {

        public InboxEntry map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            final int id = rs.getInt("id");
            final Coordinates coordinates = getCoordinates(rs);
            final String title = getTitle(rs);
            final boolean done = rs.getBoolean("done");
            final String problemReportType = rs.getString("problemReportType");
            final String extension = rs.getString("extension");
            Boolean active = rs.getBoolean("active");
            if (rs.wasNull()) {
                active = null;
            }
            Long crc32 = rs.getLong("crc32");
            if (rs.wasNull()) {
                crc32 = null;
            }
            return new InboxEntry(id, rs.getString("countryCode"), rs.getString("stationId"), title,
                    coordinates, rs.getInt("photographerId"), rs.getString("photographerNickname"), rs.getString("photographerEmail"),
                    extension, rs.getString("comment"), rs.getString("rejectReason"),
                    rs.getLong("createdAt"), done, null, rs.getString("url") != null,
                    rs.getInt("conflict") > 0,
                    problemReportType != null ? ProblemReportType.valueOf(problemReportType) : null, active,
                    crc32, rs.getBoolean("notified"));
        }

    }

    class PublicInboxEntryMapper implements RowMapper<PublicInboxEntry> {

        public PublicInboxEntry map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            final String title = getTitle(rs);
            final Coordinates coordinates = getCoordinates(rs);
            return new PublicInboxEntry(rs.getString("countryCode"), rs.getString("stationId"), title, coordinates);
        }

    }

    /**
     * Gets the uploaded title, if not present returns the station title
     */
    static String getTitle(final ResultSet rs) throws SQLException {
        String title = rs.getString("u_title");
        if (StringUtils.isBlank(title)) {
            title = rs.getString("s_title");
        }
        return title;
    }

    /**
     * Get the uploaded coordinates, if not present or not valid gets the station coordinates
     */
    static Coordinates getCoordinates(final ResultSet rs) throws SQLException {
        Coordinates coordinates = new Coordinates(rs.getDouble("u_lat"), rs.getDouble("u_lon"));
        if (!coordinates.isValid()) {
            coordinates = new Coordinates(rs.getDouble("s_lat"), rs.getDouble("s_lon"));
        }
        return coordinates;
    }

}
