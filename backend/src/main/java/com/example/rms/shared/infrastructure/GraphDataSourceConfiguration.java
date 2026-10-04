package com.example.rms.shared.infrastructure;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
/** JPA, Flyway and the transaction manager see the same pin-aware DataSource. */
@Configuration @EnableConfigurationProperties(DataSourceProperties.class)
public class GraphDataSourceConfiguration {
    @Bean(name="rmsGraphPool",destroyMethod="close") @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource graphPool(DataSourceProperties properties) { return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build(); }
    @Bean @Primary PinnedDataSource dataSource(@Qualifier("rmsGraphPool") HikariDataSource pool) { return new PinnedDataSource(pool); }
}
