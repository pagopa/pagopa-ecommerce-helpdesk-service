package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodRequestEmailDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class PMPaymentMethodsDataProviderTest {
    private val connectionFactory =
        H2ConnectionFactory(
            H2ConnectionConfiguration.builder()
                .inMemory("...")
                .option("INIT=runscript from './src/test/resources/h2scriptFile.sql'")
                .build()
        )

    private val pmPaymentMethodDataProvider = PMPaymentMethodsDataProvider(connectionFactory)

    @Test
    fun `Should find wallet for an user searching by email`() {
        val searchParam =
            SearchPaymentMethodRequestEmailDto().type("email").userEmail("test@test.it")
        StepVerifier.create(pmPaymentMethodDataProvider.findResult(searchParam))
            .assertNext { assertEquals(SearchPaymentMethodResponseDto(), it) }
            .verifyComplete()
    }
}
