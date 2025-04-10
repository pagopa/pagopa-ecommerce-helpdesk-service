package it.pagopa.ecommerce.helpdesk.configurations

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class WebClientsConfigTest {

    private val webClientsConfig = WebClientsConfig()

    @Test
    fun `should build pdv configuration`() {
        Assertions.assertDoesNotThrow {
            webClientsConfig.personalDataVaultApiClientEmail(
                apiBasePath = "localhost",
                personalDataVaultApiKey = "personal-data-vault-key"
            )
        }
    }
}
