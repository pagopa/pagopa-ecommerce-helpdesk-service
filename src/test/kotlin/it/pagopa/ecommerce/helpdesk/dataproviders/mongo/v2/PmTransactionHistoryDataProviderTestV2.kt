package it.pagopa.ecommerce.helpdesk.dataproviders.mongo.v2

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.PmTransactionsRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.PmTransactionHistoryDataProvider
import it.pagopa.ecommerce.helpdesk.documents.AccountingStatus
import it.pagopa.ecommerce.helpdesk.documents.PaymentStatus
import it.pagopa.ecommerce.helpdesk.documents.UserStatus
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import java.time.OffsetDateTime
import java.util.stream.Stream
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PmTransactionHistoryDataProviderTestV2 {

    private val testEmail = "test@test.it"
    private val fiscalCode = "fiscal code"
    private val pmTransactionsRepository: PmTransactionsRepository = mock()
    private val pmTransactionHistoryDataProvider: PmTransactionHistoryDataProvider =
        PmTransactionHistoryDataProvider(pmTransactionsRepository = pmTransactionsRepository)

    companion object {

        @JvmStatic
        fun differentSearchCriteria(): Stream<Arguments> =
            Stream.of(
                Arguments.of(HelpdeskTestUtilsV2.buildSearchRequestByRptId()),
                Arguments.of(HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()),
                Arguments.of(HelpdeskTestUtilsV2.buildSearchRequestByPaymentToken())
            )
    }

    @Test
    fun `Should count total transactions by email successfully`() {
        val count = 2L
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        given(pmTransactionsRepository.countTransactionsWithEmail(searchCriteria.userEmail))
            .willReturn(mono { count })
        StepVerifier.create(
                pmTransactionHistoryDataProvider.totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchCriteria,
                        confidentialMailUtils = null,
                        confidentialFiscalCodeUtils = null
                    )
                )
            )
            .expectNext(2)
            .verifyComplete()
    }

    @Test
    fun `Should count total transaction by fiscalCode successfully`() {
        val count = 2L
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserFiscalCode(fiscalCode)
        given(
                pmTransactionsRepository.countTransactionsWithUserFiscalCode(
                    searchCriteria.userFiscalCode
                )
            )
            .willReturn(mono { count })
        StepVerifier.create(
                pmTransactionHistoryDataProvider.totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchCriteria,
                        confidentialMailUtils = null,
                        confidentialFiscalCodeUtils = null
                    )
                )
            )
            .expectNext(2)
            .verifyComplete()
    }

    @ParameterizedTest
    @MethodSource("differentSearchCriteria")
    fun `Should return error for count by invalid search criteria`(
        searchTransaction: HelpDeskSearchTransactionRequestDto
    ) {
        StepVerifier.create(
                pmTransactionHistoryDataProvider.totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchTransaction,
                        confidentialMailUtils = null,
                        confidentialFiscalCodeUtils = null
                    )
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should return error for count by unknown search criteria`() {
        val searchCriteria: HelpDeskSearchTransactionRequestDto = mock()
        given(searchCriteria.type).willReturn("UNKNOWN")
        StepVerifier.create(
                pmTransactionHistoryDataProvider.totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchCriteria,
                        confidentialMailUtils = null,
                        confidentialFiscalCodeUtils = null
                    )
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should map successfully pmTransaction data into response by email`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val pageSize = 100
        val pageNumber = 0
        val pmTransaction =
            HelpdeskTestUtilsV2.buildPmTransactionHisotryResultDto(
                OffsetDateTime.now(),
                ProductDto.PM
            )
        val userInfo = pmTransaction.userInfo
        val transactioninfo = pmTransaction.transactionInfo
        val details = pmTransaction.paymentInfo.details[0]
        val pspInfo = pmTransaction.pspInfo

        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .userFiscalCode(userInfo.userFiscalCode)
                            .authenticationType(UserStatus.fromCode(userInfo.authenticationType))
                            .notificationEmail(userInfo.notificationEmail)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(OffsetDateTime.parse(transactioninfo.creationDate))
                            .status(PaymentStatus.fromCode(transactioninfo.status))
                            .statusDetails(
                                transactioninfo.statusDetails?.let { AccountingStatus.fromCode(it) }
                            )
                            .amount(transactioninfo.amount)
                            .fee(transactioninfo.fee)
                            .grandTotal(transactioninfo.grandTotal)
                            .rrn(transactioninfo.rrn)
                            .authorizationCode(transactioninfo.authorizationCode)
                            .paymentMethodName(transactioninfo.paymentMethodName)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(pmTransaction.paymentInfo.origin)
                            .idTransaction(details.idTransaction)
                            .details(
                                listOf(
                                    PaymentDetailInfoDto()
                                        .subject(details.subject)
                                        .iuv(details.iuv)
                                        .creditorInstitution(details.creditorInstitution)
                                        .paFiscalCode(details.paFiscalCode)
                                        .amount(details.amount)
                                )
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(pspInfo.pspId)
                            .businessName(pspInfo.businessName)
                            .idChannel(pspInfo.idChannel)
                    )
                    .product(ProductDto.valueOf(pmTransaction.product))
            )
        given(
                pmTransactionsRepository.findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                    email = testEmail,
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .willReturn(Flux.just(pmTransaction))
        StepVerifier.create(
                pmTransactionHistoryDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = null,
                            confidentialFiscalCodeUtils = null
                        ),
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun `Should map successfully pmTransaction data into response by fiscalCode`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserFiscalCode(fiscalCode)
        val pageSize = 100
        val pageNumber = 0
        val pmTransaction =
            HelpdeskTestUtilsV2.buildPmTransactionHisotryResultDto(
                OffsetDateTime.now(),
                ProductDto.PM
            )
        val userInfo = pmTransaction.userInfo
        val transactioninfo = pmTransaction.transactionInfo
        val details = pmTransaction.paymentInfo.details[0]
        val pspInfo = pmTransaction.pspInfo

        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .userFiscalCode(userInfo.userFiscalCode)
                            .authenticationType(UserStatus.fromCode(userInfo.authenticationType))
                            .notificationEmail(userInfo.notificationEmail)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(OffsetDateTime.parse(transactioninfo.creationDate))
                            .status(PaymentStatus.fromCode(transactioninfo.status))
                            .statusDetails(
                                transactioninfo.statusDetails?.let { AccountingStatus.fromCode(it) }
                            )
                            .amount(transactioninfo.amount)
                            .fee(transactioninfo.fee)
                            .grandTotal(transactioninfo.grandTotal)
                            .rrn(transactioninfo.rrn)
                            .authorizationCode(transactioninfo.authorizationCode)
                            .paymentMethodName(transactioninfo.paymentMethodName)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(pmTransaction.paymentInfo.origin)
                            .idTransaction(details.idTransaction)
                            .details(
                                listOf(
                                    PaymentDetailInfoDto()
                                        .subject(details.subject)
                                        .iuv(details.iuv)
                                        .creditorInstitution(details.creditorInstitution)
                                        .paFiscalCode(details.paFiscalCode)
                                        .amount(details.amount)
                                )
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(pspInfo.pspId)
                            .businessName(pspInfo.businessName)
                            .idChannel(pspInfo.idChannel)
                    )
                    .product(ProductDto.valueOf(pmTransaction.product))
            )

        given(
                pmTransactionsRepository
                    .findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
                        userFiscalCode = fiscalCode,
                        skip = pageSize * pageNumber,
                        limit = pageSize
                    )
            )
            .willReturn(Flux.just(pmTransaction))

        StepVerifier.create(
                pmTransactionHistoryDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = null,
                            confidentialFiscalCodeUtils = null
                        ),
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun `Should return error for search by unknown search criteria`() {
        val searchCriteria: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto> = mock()
        val helpDeskSearchTransactionRequestDto: HelpDeskSearchTransactionRequestDto = mock()
        given(helpDeskSearchTransactionRequestDto.type).willReturn("UNKNOWN")
        given(searchCriteria.decode()).willReturn(Mono.just(helpDeskSearchTransactionRequestDto))
        StepVerifier.create(
                pmTransactionHistoryDataProvider.findResult(
                    searchParams = searchCriteria,
                    skip = 0,
                    limit = 0
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @ParameterizedTest
    @MethodSource("differentSearchCriteria")
    fun `Should return error for search by invalid search criteria`(
        searchTransaction: HelpDeskSearchTransactionRequestDto
    ) {
        val searchParamDecoder: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto> = mock()
        given(searchParamDecoder.decode()).willReturn(Mono.just(searchTransaction))
        StepVerifier.create(
                pmTransactionHistoryDataProvider.findResult(
                    searchParams = searchParamDecoder,
                    skip = 0,
                    limit = 0
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `Should return the input number as string for UserStatus enum`() =
        assertEquals("13", UserStatus.fromCode(13))

    @Test
    fun `Should return the input number as string for PaymentStatus enum`() =
        assertEquals("22", PaymentStatus.fromCode(22))

    @Test
    fun `Should return the input number as string for AccountingStatus enum`() =
        assertEquals("6", AccountingStatus.fromCode(6))
}
