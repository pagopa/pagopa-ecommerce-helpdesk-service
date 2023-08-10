package it.pagopa.ecommerce.helpdesk.configurations

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class OracleConfigurationTest {

    private val oracleConfiguration = OracleConfiguration()

    @Test
    fun `should build connection factory successfully`() {
        assertDoesNotThrow {
            oracleConfiguration.getPMConnectionFactory(
                dbHost = "127.0.0.1",
                dbPort = 1521,
                databaseName = "database",
                username = "username",
                password = "password"
            )
        }
    }
}
