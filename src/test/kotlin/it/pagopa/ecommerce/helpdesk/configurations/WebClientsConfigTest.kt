package it.pagopa.ecommerce.helpdesk.configurations

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class WebClientsConfigTest {

    private val webClientsConfig = WebClientsConfig()

    @Test
    fun `should build pdv configuration - email`() {
        Assertions.assertDoesNotThrow {
            webClientsConfig.personalDataVaultApiClientEmail(
                apiBasePath = "localhost",
                personalDataVaultApiKey = "personal-data-vault-key"
            )
        }
    }

    @Test
    fun `should build pdv configuration - fiscal code`() {
        Assertions.assertDoesNotThrow {
            webClientsConfig.personalDataVaultApiClientFiscalCode(
                apiBasePath = "localhost",
                personalDataVaultApiKey = "personal-data-vault-key"
            )
        }
    }
}
