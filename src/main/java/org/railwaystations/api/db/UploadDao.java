package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Upload;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public interface UploadDao {

    String JOIN_QUERY = "select u.id, u.countryCode, u.stationId, u.title u_title, s.title s_title, u.lat u_lat, u.lon u_lon, s.lat s_lat, s.lon s_lon, u.photographerId, p.name photographerNickname, u.extension, u.uploadComment, u.rejectReason, u.createdAt, u.done from uploads u left join stations s on s.countryCode = u.countryCode and s.id = u.stationId left join users p on p.id = u.photographerId";

    @SqlQuery(JOIN_QUERY + " where u.id = :id")
    @RegisterRowMapper(UploadMapper.class)
    Set<Station> findById(@BindList("id") final int id);

    @SqlQuery(JOIN_QUERY + " where u.done = false")
    @RegisterRowMapper(UploadMapper.class)
    Set<Station> findOpenUploads();

    @SqlQuery(JOIN_QUERY)
    @RegisterRowMapper(UploadMapper.class)
    Set<Station> all();

    @SqlUpdate("insert into uploads (id, countryCode, stationId, title, lat, lon, photographerId, extension, uploadComment, done, createdAt) values (:id, :countryCode, :stationId, :title, :coordinates.lat, :coordinates.lon, :photographerId, :extension, :uploadComment, :done, :createdAt)")
    @GetGeneratedKeys("id")
    Integer insert(@BindBean final Upload upload);

    @SqlUpdate("update uploads set countryCode = :countryCode, stationId = :stationId, rejectReason = :rejectReason, done = :done where id = :id")
    void update(@BindBean final Upload upload);

    class UploadMapper implements RowMapper<Upload> {

        private static String inboxBaseUrl = "";

        public static void setInboxBaseUrl(final String url) {
            inboxBaseUrl = url;
        }

        public Upload map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            int id = rs.getInt("id");
            String countryCode = rs.getString("countryCode");
            String stationId = rs.getString("stationId");
            Coordinates coordinates;
            String title;
            if (countryCode != null && stationId != null) {
                coordinates = new Coordinates(rs.getDouble("s_lat"), rs.getDouble("s_lon"));
                title = rs.getString("s_title");
            } else {
                coordinates = new Coordinates(rs.getDouble("u_lat"), rs.getDouble("u_lon"));
                title = rs.getString("u_title");
            }
            boolean done = rs.getBoolean("done");
            String extension = rs.getString("extension");
            String photoUrl = null;
            if (!done) {
                photoUrl = inboxBaseUrl + "/" + id + extension;
            }
            return new Upload(id, rs.getString("countryCode"), rs.getString("stationId"), title,
                    coordinates, rs.getInt("photographerId"), rs.getString("photographerNickname"),
                    extension, photoUrl, rs.getString("uploadComment"), rs.getString("rejectReason"),
                    rs.getLong("createdAt"), done, null);
        }

    }

}
