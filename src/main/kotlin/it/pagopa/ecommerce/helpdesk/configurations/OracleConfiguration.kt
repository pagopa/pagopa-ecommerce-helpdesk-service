package it.pagopa.ecommerce.helpdesk.configurations

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import java.nio.CharBuffer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OracleConfiguration {

    @Bean
    fun getPMConnectionFactory(
        @Value("\${pm.oracle.host}") dbHost: String,
        @Value("\${pm.oracle.port}") dbPort: Int,
        @Value("\${pm.oracle.databaseName}") databaseName: String,
        @Value("\${pm.oracle.userName}") username: String,
        @Value("\${pm.oracle.password}") password: String
    ): ConnectionFactory =
        ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "oracle")
                .option(ConnectionFactoryOptions.HOST, dbHost)
                .option(ConnectionFactoryOptions.PORT, dbPort)
                .option(ConnectionFactoryOptions.DATABASE, databaseName)
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, CharBuffer.wrap(password))
                .build()
        )
}
