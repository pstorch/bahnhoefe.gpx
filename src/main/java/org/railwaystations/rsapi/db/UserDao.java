package org.railwaystations.rsapi.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.railwaystations.rsapi.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    @SqlQuery("select * from users")
    @RegisterRowMapper(UserMapper.class)
    List<User> list();

    @SqlQuery("select * from users where normalizedName = :normalizedName")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByNormalizedName(@Bind("normalizedName") final String normalizedName);

    @SqlQuery("select * from users where id = :id")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findById(@Bind("id") final int id);

    @SqlQuery("select * from users where email = :email")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByEmail(@Bind("email") final String email);

    @SqlUpdate("update users set uploadTokenSalt = null, `key` = :key where id = :id")
    void updateCredentials(@Bind("id") final int id, @Bind("key") final String key);

    @SqlUpdate("insert into users (id, name, url, license, email, normalizedName, ownPhotos, anonymous, `key`, emailVerification, sendNotifications) values (:id, :name, :url, :license, :email, :normalizedName, :ownPhotos, :anonymous, :key, :emailVerification, :sendNotifications)")
    @GetGeneratedKeys("id")
    Integer insert(@BindBean final User user);

    @SqlUpdate("update users set name = :name, url = :url, license = :license, email = :email, normalizedName = :normalizedName, ownPhotos = :ownPhotos, anonymous = :anonymous, emailVerification = :emailVerification, sendNotifications = :sendNotifications where id = :id")
    void update(@BindBean final User user);

    @SqlQuery("select * from users where emailVerification = :emailVerification")
    @RegisterRowMapper(UserMapper.class)
    Optional<User> findByEmailVerification(@Bind("emailVerification") final String emailVerification);

    @SqlUpdate("update users set emailVerification = :emailVerification where id = :id")
    void updateEmailVerification(@Bind("id") final int id, @Bind("emailVerification") final String emailVerification);

    class UserMapper implements RowMapper<User> {
        public User map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new User(rs.getString("name"),
                    rs.getString("url"),
                    rs.getString("license"),
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getBoolean("ownPhotos"),
                    rs.getBoolean("anonymous"),
                    rs.getLong("uploadTokenSalt"),
                    rs.getString("key"),
                    rs.getBoolean("admin"),
                    rs.getString("emailVerification"),
                    rs.getBoolean("sendNotifications")
                    );
        }
    }

}
