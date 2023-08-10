package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import reactor.core.publisher.Hooks
import reactor.test.StepVerifier

class PMTransactionDataProviderTest {

    private val connectionFactory =
        H2ConnectionFactory(
            H2ConnectionConfiguration.builder()
                .inMemory("...")
                .option("INIT=runscript from './src/test/resources/h2scriptFile.sql'")
                .build()
        )

    private val pmTransactionDataProvider = PMTransactionDataProvider(connectionFactory)

    @Test
    fun `Should count total record successfully`() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    HelpdeskTestUtils.buildSearchRequestByUserMail()
                )
            )
            .expectNext(1)
            .verifyComplete()
    }

    @Test
    fun `Should count found result for paginated query`() {
        val expectedResponse =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .userFiscalCode("fiscal_code")
                            .notificationEmail("test@test.it")
                            .surname("surname")
                            .name("name")
                            .username("username")
                            .authenticationType("Utente non registrato")
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(OffsetDateTime.parse("2018-06-26T17:05:36.232+02:00"))
                            .status("In attesa mod1")
                            .statusDetails("Contabilizzato")
                            .amount(100)
                            .fee(50)
                            .grandTotal(150)
                            .rrn("rrn")
                            .authotizationCode("auth code")
                            .paymentMethodName("payment method name")
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin("origin")
                            .subject("/RFB/718173815252003/0.10/TXT/Pagamento di test 20")
                    )
                    .paymentDetailInfo(
                        PaymentDetailInfoDto()
                            .iuv("000000044060814")
                            .idTransaction("MIUR20191119222949")
                            .creditorInstitution("RMIC81500N")
                            .paFiscalCode("97061100588")
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId("CCRTIT2TCAR")
                            .businessName("Test")
                            .idChannel("97735020584_01")
                    )
                    .product(ProductDto.PM)
            )
        StepVerifier.create(
                pmTransactionDataProvider.findResult(
                    searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(),
                    pageNumber = 0,
                    pageSize = 10
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()
    }

    @Test
    fun `Should return 0 for unhandled search criteria for count operation`() {

        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
                )
            )
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `Should return error for invalid input search criteria for find result operation`() {

        StepVerifier.create(
                pmTransactionDataProvider.findResult(
                    searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId(),
                    pageNumber = 0,
                    pageSize = 10
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }
}
