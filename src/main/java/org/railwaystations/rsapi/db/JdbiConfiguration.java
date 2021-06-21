package org.railwaystations.rsapi.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class JdbiConfiguration {

    @Bean
    public Jdbi jdbi(final DataSource ds, final List<JdbiPlugin> jdbiPlugins, final List<RowMapper<?>> rowMappers) {
        final TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(ds);
        final Jdbi jdbi = Jdbi.create(proxy);
        jdbiPlugins.forEach(jdbi::installPlugin);
        rowMappers.forEach(jdbi::registerRowMapper);
        return jdbi;
    }

    @Bean
    public CountryDao countryDao(final Jdbi jdbi) {
        return jdbi.onDemand(CountryDao.class);
    }

    @Bean
    public InboxDao inboxDao(final Jdbi jdbi) {
        return jdbi.onDemand(InboxDao.class);
    }

    @Bean
    public PhotoDao photoDao(final Jdbi jdbi) {
        return jdbi.onDemand(PhotoDao.class);
    }

    @Bean
    public StationDao stationDao(final Jdbi jdbi) {
        return jdbi.onDemand(StationDao.class);
    }

    @Bean
    public UserDao userDao(final Jdbi jdbi) {
        return jdbi.onDemand(UserDao.class);
    }

}
