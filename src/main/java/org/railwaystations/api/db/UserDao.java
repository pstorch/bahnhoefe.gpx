package org.railwaystations.api.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.railwaystations.api.model.User;

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

    class UserMapper implements RowMapper<User> {
        public UserMapper() {
        }

        public User map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new User(rs.getString("name"),
                    rs.getString("url"),
                    rs.getString("license"),
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("normalizedName"),
                    rs.getBoolean("ownPhotos"),
                    rs.getBoolean("anonymous"),
                    rs.getLong("uploadTokenSalt")
                    );
        }

    }

}
