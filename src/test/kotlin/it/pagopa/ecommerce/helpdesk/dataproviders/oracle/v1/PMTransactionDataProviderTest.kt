package it.pagopa.ecommerce.helpdesk.dataproviders.oracle.v1

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
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
                    SearchParamDecoder(
                        searchParameter =
                            HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it"),
                        confidentialMailUtils = null
                    )
                )
            )
            .expectNext(1)
            .verifyComplete()
    }

    @Test
    fun `Should handle no transaction found by email`() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    SearchParamDecoder(
                        searchParameter =
                            HelpdeskTestUtils.buildSearchRequestByUserMail("unknown@test.it"),
                        confidentialMailUtils = null
                    ),
                )
            )
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `Should thrown error counting transaction for unhandled search criteria `() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    SearchParamDecoder(
                        searchParameter = HelpdeskTestUtils.buildSearchRequestByTransactionId(),
                        confidentialMailUtils = null
                    ),
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should thrown error counting transaction for unknown search criteria `() {
        Hooks.onOperatorDebug()
        val searchCriteria: HelpDeskSearchTransactionRequestDto = mock()
        given(searchCriteria.type).willReturn("UNKNOWN")
        StepVerifier.create(
                pmTransactionDataProvider.findResult(
                    searchParams = SearchParamDecoder(searchParameter = searchCriteria, null),
                    skip = 0,
                    limit = 0
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should count found result for paginated query for email transaction search`() {
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
                            .authenticationType("Utente registrato SPID")
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
                            .authorizationCode("auth code")
                            .paymentMethodName("payment method name")
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin("origin")
                            .details(
                                listOf(
                                    PaymentDetailInfoDto()
                                        .subject(
                                            "/RFB/718173815252003/0.10/TXT/Pagamento di test 20"
                                        )
                                        .iuv("000000044060814")
                                        .idTransaction("MIUR20191119222949")
                                        .creditorInstitution("RMIC81500N")
                                        .paFiscalCode("97061100588")
                                )
                            )
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
                    searchParams =
                        SearchParamDecoder(
                            searchParameter =
                                HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it"),
                            confidentialMailUtils = null
                        ),
                    limit = 10,
                    skip = 0
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()
    }

    @Test
    fun `Should count total record successfully for user fiscal code transaction search`() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    SearchParamDecoder(
                        searchParameter =
                            HelpdeskTestUtils.buildSearchRequestByUserFiscalCode("fiscal_code"),
                        confidentialMailUtils = null
                    ),
                )
            )
            .expectNext(1)
            .verifyComplete()
    }

    @Test
    fun `Should handle no transaction found by user fiscal code`() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    SearchParamDecoder(
                        searchParameter =
                            HelpdeskTestUtils.buildSearchRequestByUserFiscalCode(
                                "unknown-fiscal_code"
                            ),
                        confidentialMailUtils = null
                    ),
                )
            )
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `Should count found result for paginated query for user fiscal code transaction search`() {
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
                            .authenticationType("Utente registrato SPID")
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
                            .authorizationCode("auth code")
                            .paymentMethodName("payment method name")
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin("origin")
                            .details(
                                listOf(
                                    PaymentDetailInfoDto()
                                        .subject(
                                            "/RFB/718173815252003/0.10/TXT/Pagamento di test 20"
                                        )
                                        .iuv("000000044060814")
                                        .idTransaction("MIUR20191119222949")
                                        .creditorInstitution("RMIC81500N")
                                        .paFiscalCode("97061100588")
                                )
                            )
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
                    searchParams =
                        SearchParamDecoder(
                            searchParameter =
                                HelpdeskTestUtils.buildSearchRequestByUserFiscalCode("fiscal_code"),
                            confidentialMailUtils = null
                        ),
                    limit = 10,
                    skip = 0
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()
    }

    @Test
    fun `Should return error for unhandled search criteria for count operation`() {

        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    searchParams =
                        SearchParamDecoder(
                            searchParameter = HelpdeskTestUtils.buildSearchRequestByRptId(),
                            confidentialMailUtils = null
                        ),
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should return error for unknown search criteria for count operation`() {
        val searchCriteria: HelpDeskSearchTransactionRequestDto = mock()
        given(searchCriteria.type).willReturn("UNKNOWN")
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    searchParams = SearchParamDecoder(searchParameter = searchCriteria, null),
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should return error for invalid input search criteria for find result operation`() {

        StepVerifier.create(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoder(
                            searchParameter = HelpdeskTestUtils.buildSearchRequestByRptId(),
                            confidentialMailUtils = null
                        ),
                    limit = 0,
                    skip = 10
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should count total record successfully for transactions within date range`() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.totalRecordCount(
                    SearchParamDecoder(
                        searchParameter =
                            HelpdeskTestUtils.buildSearchTransactionRequestDateTimeRangeDto(
                                type = "DATE_TIME_RANGE",
                                timeRangeDto =
                                    SearchTransactionRequestDateTimeRangeDto()
                                        .startDate(
                                            OffsetDateTime.parse("2018-06-25T00:00:00+02:00")
                                        )
                                        .endDate(OffsetDateTime.parse("2018-06-27T00:00:00+02:00"))
                            ),
                        confidentialMailUtils = null
                    )
                )
            )
            .expectNext(1)
            .verifyComplete()
    }

    @Test
    fun `Should handle no transactions found within date range`() {
        Hooks.onOperatorDebug()
        StepVerifier.create(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoder(
                            searchParameter =
                                HelpdeskTestUtils.buildSearchTransactionRequestDateTimeRangeDto(
                                    type = "DATE_RANGE",
                                    timeRangeDto =
                                        SearchTransactionRequestDateTimeRangeDto()
                                            .startDate(
                                                OffsetDateTime.parse("2025-06-25T00:00:00+02:00")
                                            )
                                            .endDate(
                                                OffsetDateTime.parse("2025-06-27T00:00:00+02:00")
                                            )
                                ),
                            confidentialMailUtils = null
                        ),
                    limit = 10,
                    skip = 0
                )
            )
            .expectNextMatches { it.isEmpty() }
            .verifyComplete()
    }

    @Test
    fun `Should throw InvalidSearchCriteriaException for unsupported search type in date range`() {
        Hooks.onOperatorDebug()
        val searchCriteria: HelpDeskSearchTransactionRequestDto = mock()
        given(searchCriteria.type).willReturn("UNKNOWN")
        StepVerifier.create(
                pmTransactionDataProvider.findResult(
                    searchParams = SearchParamDecoder(searchParameter = searchCriteria, null),
                    skip = 0,
                    limit = 0
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }
}
