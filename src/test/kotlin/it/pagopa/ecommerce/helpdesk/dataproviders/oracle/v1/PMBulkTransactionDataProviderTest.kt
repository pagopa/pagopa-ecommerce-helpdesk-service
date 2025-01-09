package it.pagopa.ecommerce.helpdesk.dataproviders.oracle.v1

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMBulkTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class PMBulkTransactionDataProviderTest {
    private val connectionFactory =
        H2ConnectionFactory(
            H2ConnectionConfiguration.builder()
                .inMemory("...")
                .option("INIT=runscript from './src/test/resources/h2scriptFile.sql'")
                .build()
        )

    private val pmBulkTransactionDataProvider = PMBulkTransactionDataProvider(connectionFactory)

    @Test
    fun `Should handle no bulk transaction found by transactionId range`() {

        StepVerifier.create(
                pmBulkTransactionDataProvider.findResult(
                    SearchTransactionRequestTransactionIdRangeDto()
                        .type("TRANSACTION_ID_RANGE")
                        .transactionIdRange(
                            SearchTransactionRequestTransactionIdRangeTransactionIdRangeDto()
                                .startTransactionId("10")
                                .endTransactionId("1")
                        ),
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()
    }

    @Test
    fun `Should handle bulk transaction found by transactionId range`() {

        val expectedCountResult = 1

        StepVerifier.create(
                pmBulkTransactionDataProvider.findResult(
                    SearchTransactionRequestTransactionIdRangeDto()
                        .type("TRANSACTION_ID_RANGE")
                        .transactionIdRange(
                            SearchTransactionRequestTransactionIdRangeTransactionIdRangeDto()
                                .startTransactionId("1")
                                .endTransactionId("2")
                        ),
                )
            )
            .assertNext { assertEquals(expectedCountResult, it.size) }
            .verifyComplete()
    }
}
