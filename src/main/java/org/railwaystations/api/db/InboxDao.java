package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.InboxEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface InboxDao {

    String JOIN_QUERY = "select u.id, u.countryCode, u.stationId, u.title u_title, s.title s_title, u.lat u_lat, u.lon u_lon, s.lat s_lat, s.lon s_lon, "
                    + "     u.photographerId, p.name photographerNickname, u.extension, u.comment, u.rejectReason, u.createdAt, u.done, u.problemReport, f.url, "
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

    @SqlQuery(JOIN_QUERY)
    @RegisterRowMapper(InboxEntryMapper.class)
    Set<InboxEntry> all();

    @SqlUpdate("insert into inbox (countryCode, stationId, title, lat, lon, photographerId, extension, comment, done, createdAt, problemReport) values (:countryCode, :stationId, :title, :coordinates?.lat, :coordinates?.lon, :photographerId, :extension, :comment, :done, :createdAt, :problemReport)")
    @GetGeneratedKeys("id")
    Integer insert(@BindBean final InboxEntry inboxEntry);

    @SqlUpdate("update inbox set countryCode = :countryCode, stationId = :stationId, done = true where id = :id")
    void done(@Bind("id") final int id, @Bind("countryCode") final String countryCode, @Bind("stationId") final String stationId);

    @SqlUpdate("update inbox set rejectReason = :rejectReason, done = true where id = :id")
    void reject(@Bind("id") final int id, @Bind("rejectReason") final String rejectReason);

    @SqlUpdate("update inbox set done = true where id = :id")
    void done(@Bind("id") int id);

    @SqlQuery("select count(*) from inbox where countryCode = :countryCode and stationId = :stationId and done = false and photographerId != :photographerId")
    int countPendingInboxEntriesForStationOfOtherUser(@Bind("countryCode") final String countryCode, @Bind("stationId") final String stationId, @Bind("photographerId") final int photographerId);

    @SqlQuery("select count(*) from inbox where done = false")
    int countPendingInboxEntries();

    class InboxEntryMapper implements RowMapper<InboxEntry> {

        public InboxEntry map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            final int id = rs.getInt("id");
            final String countryCode = rs.getString("countryCode");
            final String stationId = rs.getString("stationId");
            final Coordinates coordinates;
            final String title;
            if (countryCode != null && stationId != null) {
                coordinates = new Coordinates(rs.getDouble("s_lat"), rs.getDouble("s_lon"));
                title = rs.getString("s_title");
            } else {
                coordinates = new Coordinates(rs.getDouble("u_lat"), rs.getDouble("u_lon"));
                title = rs.getString("u_title");
            }
            final boolean done = rs.getBoolean("done");
            final boolean problemReport = rs.getBoolean("problemReport");
            final String extension = rs.getString("extension");
            return new InboxEntry(id, rs.getString("countryCode"), rs.getString("stationId"), title,
                    coordinates, rs.getInt("photographerId"), rs.getString("photographerNickname"),
                    extension, rs.getString("comment"), rs.getString("rejectReason"),
                    rs.getLong("createdAt"), done, null, rs.getString("url") != null,
                    rs.getInt("conflict") > 0, problemReport);
        }

    }

}
