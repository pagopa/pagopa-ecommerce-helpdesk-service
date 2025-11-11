package it.pagopa.ecommerce.helpdesk.dataproviders.mongo.v2

import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData as TransactionAuthorizationRequestDataV2
import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v2.TransactionClosureData as TransactionClosureDataV2
import it.pagopa.ecommerce.commons.documents.v2.TransactionEvent as TransactionEventV2
import it.pagopa.ecommerce.commons.documents.v2.TransactionUserReceiptData as TransactionUserReceiptDataV2
import it.pagopa.ecommerce.commons.documents.v2.activation.NpgTransactionGatewayActivationData
import it.pagopa.ecommerce.commons.documents.v2.authorization.NpgTransactionGatewayAuthorizationData
import it.pagopa.ecommerce.commons.documents.v2.authorization.RedirectTransactionGatewayAuthorizationData
import it.pagopa.ecommerce.commons.documents.v2.authorization.TransactionGatewayAuthorizationData
import it.pagopa.ecommerce.commons.documents.v2.authorization.TransactionGatewayAuthorizationRequestedData
import it.pagopa.ecommerce.commons.documents.v2.refund.NpgGatewayRefundData
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.v2.Email
import it.pagopa.ecommerce.commons.domain.v2.TransactionWithUserReceiptOk as TransactionWithUserReceiptOkV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionExpired as BaseTransactionExpiredV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithClosureError as BaseTransactionWithClosureErrorV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithCompletedAuthorization as BaseTransactionWithCompletedAuthorizationV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRefundRequested as BaseTransactionWithRefundRequestedV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRequestedAuthorization as BaseTransactionWithRequestedAuthorizationV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRequestedUserReceipt as BaseTransactionWithRequestedUserReceiptV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithUserReceipt as BaseTransactionWithUserReceiptV2
import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils as TransactionTestUtilsV2
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2.convertEventsToEventInfoList
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreHistoryRepository as TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewHistoryRepository as TransactionsViewHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.getGatewayAuthorizationData
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import java.time.ZonedDateTime
import java.util.*
import java.util.stream.Stream
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceForTransactionV2DataProviderWithHistoryTest {

    companion object {
        val testedStatuses: MutableSet<TransactionStatusDto> = HashSet()
        const val TEST_EMAIL = "test.email@test.it"
        const val TEST_FISCAL_CODE = "fiscalcode"

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            testedStatuses.clear()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            TransactionStatusDto.values().forEach {
                Assertions.assertTrue(
                    testedStatuses.contains(it),
                    "Error: Transaction in status [$it] NOT covered by tests!"
                )
            }
            testedStatuses.clear()
        }

        @JvmStatic
        /**
         * Test arguments paymentGateway: TransactionAuthorizationRequestData.PaymentGateway,
         * gatewayAuthRequestedData: TransactionGatewayAuthorizationRequestedData, gatewayAuthData:
         * TransactionGatewayAuthorizationData
         */
        fun differentPaymentGatewayDataTestMethodSourceAuthOK(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.XPAY,
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationData(
                        AuthorizationResultDto.OK
                    ),
                    "VISA"
                ),
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.VPOS,
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationData(
                        AuthorizationResultDto.OK
                    ),
                    "VISA"
                ),
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.NPG,
                    TransactionTestUtilsV2.npgTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                        OperationResultDto.EXECUTED
                    ),
                    "VISA"
                ),
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT,
                    TransactionTestUtilsV2.redirectTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.redirectTransactionGatewayAuthorizationData(
                        RedirectTransactionGatewayAuthorizationData.Outcome.OK,
                        null
                    ),
                    "N/A"
                )
            )

        private const val authKoErrorCode = "errorCode"

        @JvmStatic
        /**
         * Test arguments paymentGateway: TransactionAuthorizationRequestData.PaymentGateway,
         * gatewayAuthRequestedData: TransactionGatewayAuthorizationRequestedData, gatewayAuthData:
         * TransactionGatewayAuthorizationData, expectedErrorCode: String?
         */
        fun differentPaymentGatewayDataTestMethodSourceAuthKO(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.XPAY,
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationData(
                        AuthorizationResultDto.KO,
                        authKoErrorCode
                    ),
                    "VISA",
                    authKoErrorCode
                ),
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.VPOS,
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.pgsTransactionGatewayAuthorizationData(
                        AuthorizationResultDto.KO,
                        authKoErrorCode
                    ),
                    "VISA",
                    authKoErrorCode
                ),
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.NPG,
                    TransactionTestUtilsV2.npgTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                        OperationResultDto.FAILED
                    ),
                    "VISA",
                    null
                ),
                Arguments.of(
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT,
                    TransactionTestUtilsV2.redirectTransactionGatewayAuthorizationRequestedData(),
                    TransactionTestUtilsV2.redirectTransactionGatewayAuthorizationData(
                        RedirectTransactionGatewayAuthorizationData.Outcome.KO,
                        authKoErrorCode
                    ),
                    "N/A",
                    authKoErrorCode
                )
            )

        /** Test data userId: String? expectedAuthType: String */
        @JvmStatic
        fun registeredOrGuestTestMethodSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(UUID.randomUUID().toString(), "REGISTERED"),
                Arguments.of(null, "GUEST")
            )
    }

    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsViewHistoryRepository: TransactionsViewHistoryRepository = mock()

    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()
    private val transactionsEventStoreHistoryRepository:
        TransactionsEventStoreHistoryRepository<Any> =
        mock()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val ecommerceTransactionDataProvider =
        EcommerceTransactionDataProvider(
            transactionsViewRepository,
            transactionsViewHistoryRepository,
            transactionsEventStoreRepository,
            transactionsEventStoreHistoryRepository
        )

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in ACTIVATED state`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val events = listOf(transactionActivatedEvent) as List<TransactionEventV2<Any>>
        val baseTransaction = TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.empty())
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in AUTHORIZATION_REQUESTED state`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.AUTHORIZATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val events =
            listOf(transactionActivatedEvent, transactionAuthorizationRequestedEvent)
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(1)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(1)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @ParameterizedTest
    @MethodSource("differentPaymentGatewayDataTestMethodSourceAuthKO")
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in AUTHORIZATION_COMPLETED KO state`(
        paymentGateway: TransactionAuthorizationRequestData.PaymentGateway,
        gatewayAuthRequestedData: TransactionGatewayAuthorizationRequestedData,
        gatewayAuthData: TransactionGatewayAuthorizationData,
        brand: String,
        expectedErrorCode: String?
    ) {
        val gatewayAuthorizationData = getGatewayAuthorizationData(gatewayAuthData)
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.AUTHORIZATION_COMPLETED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                paymentGateway,
                gatewayAuthRequestedData
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(gatewayAuthData)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(1)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(1)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Rifiutato")
                            .statusDetails(expectedErrorCode)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as? NpgTransactionGatewayAuthorizationData)
                                    ?.operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(brand)
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(
                                gatewayAuthorizationData?.authorizationStatus
                            )
                            .gatewayErrorCode(gatewayAuthorizationData?.errorCode)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Rifiutato")
                            .statusDetails(expectedErrorCode)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as? NpgTransactionGatewayAuthorizationData)
                                    ?.operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(brand)
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(
                                gatewayAuthorizationData?.authorizationStatus
                            )
                            .gatewayErrorCode(gatewayAuthorizationData?.errorCode)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in CLOSED state`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in CLOSED state outcome KO`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.KO)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in CLOSURE_REQUESTED`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSURE_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in CLOSURE_ERROR state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSURE_ERROR,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val closureErrorEvent = TransactionTestUtilsV2.transactionClosureErrorEvent()
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                closureErrorEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            ((TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                    as BaseTransactionWithClosureErrorV2))
                .transactionAtPreviousState as BaseTransactionWithCompletedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in CANCELLATION_REQUESTED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CANCELLATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionCancellationRequestedEvent =
            TransactionTestUtilsV2.transactionUserCanceledEvent()
        val events =
            listOf(transactionActivatedEvent, transactionCancellationRequestedEvent)
                as List<TransactionEventV2<Any>>
        val baseTransaction = (TransactionTestUtilsV2.reduceEvents(*events.toTypedArray()))
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(1)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(1)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in CANCELLATION_EXPIRED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CANCELLATION_EXPIRED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionCancellationRequestedEvent =
            TransactionTestUtilsV2.transactionUserCanceledEvent()
        val transactionExpiredEvent =
            TransactionTestUtilsV2.transactionExpiredEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionCancellationRequestedEvent
                )
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionCancellationRequestedEvent,
                transactionExpiredEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction = (TransactionTestUtilsV2.reduceEvents(*events.toTypedArray()))
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(1)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(1)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in CANCELED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CANCELED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionCancellationRequestedEvent =
            TransactionTestUtilsV2.transactionUserCanceledEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionCancellationRequestedEvent,
                transactionClosedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction = (TransactionTestUtilsV2.reduceEvents(*events.toTypedArray()))
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(1)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(1)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in UNAUTHORIZED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.UNAUTHORIZED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.DECLINED,
                    "101"
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionCloseFailedEvent =
            TransactionTestUtilsV2.transactionClosureFailedEvent(
                TransactionClosureDataV2.Outcome.KO
            )

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionCloseFailedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorizationV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Rifiutato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.DECLINED.value)
                            .gatewayErrorCode("101")
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Rifiutato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.DECLINED.value)
                            .gatewayErrorCode("101")
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in NOTIFICATION_REQUESTED OK state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction data into response searching by transaction id for transaction in NOTIFICATION_REQUESTED KO state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))

        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in NOTIFICATION_ERROR OK state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddErrorEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in NOTIFICATION_ERROR KO state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddErrorEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionClosedRequestedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in NOTIFIED_KO state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFIED_KO,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction data into response searching by transaction id for transaction in REFUND_REQUESTED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUND_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )

        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionRefundRequestedEvent =
            TransactionTestUtilsV2.transactionRefundRequestedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedEvent,
                    transactionClosedRequestedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                ),
                transactionAuthorizationCompletedEvent.data.transactionGatewayAuthorizationData
            )

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionClosedRequestedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError,
                transactionRefundRequestedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            (TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                    as BaseTransactionWithRefundRequestedV2)
                .transactionAtPreviousState as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in REFUND_ERROR state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUND_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionRefundRequestedEvent =
            TransactionTestUtilsV2.transactionRefundRequestedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                ),
                transactionAuthorizationCompletedEvent.data.transactionGatewayAuthorizationData
            )
        val transactionRefundErrorEvent =
            TransactionTestUtilsV2.transactionRefundErrorEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionRefundRequestedEvent
                )
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError,
                transactionRefundRequestedEvent,
                transactionRefundErrorEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransactionRefundError =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithUserReceiptV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (baseTransactionRefundRequested.transactionAuthorizationGatewayData
                                        .get() as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (baseTransactionRefundRequested.transactionAuthorizationGatewayData
                                    .get() as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in REFUNDED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val refundOperationId = "refundOperationId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionRefundRequestedEvent =
            TransactionTestUtilsV2.transactionRefundRequestedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                ),
                transactionAuthorizationCompletedEvent.data.transactionGatewayAuthorizationData
            )
        val transactionRefundErrorEvent =
            TransactionTestUtilsV2.transactionRefundErrorEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionRefundRequestedEvent
                )
            )
        val transactionRefundedEvent =
            TransactionTestUtilsV2.transactionRefundedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionRefundRequestedEvent,
                    transactionRefundErrorEvent
                ),
                NpgGatewayRefundData(refundOperationId)
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError,
                transactionRefundRequestedEvent,
                transactionRefundErrorEvent,
                transactionRefundedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransactionRefunded =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundError =
            baseTransactionRefunded.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithUserReceiptV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(refundOperationId)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(refundOperationId)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in REFUNDED FROM EXPIRED state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val refundOperationId = "refundOperationId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionExpiredEvent =
            TransactionTestUtilsV2.transactionExpiredEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
            )
        val transactionRefundRequestedEvent =
            TransactionTestUtilsV2.transactionRefundRequestedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionExpiredEvent
                ),
                transactionAuthorizationCompletedEvent.data.transactionGatewayAuthorizationData
            )
        val transactionRefundErrorEvent =
            TransactionTestUtilsV2.transactionRefundErrorEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionExpiredEvent,
                    transactionRefundRequestedEvent
                )
            )
        val transactionRefundedEvent =
            TransactionTestUtilsV2.transactionRefundedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionExpiredEvent,
                    transactionRefundRequestedEvent,
                    transactionRefundErrorEvent
                ),
                NpgGatewayRefundData(refundOperationId)
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError,
                transactionExpiredEvent,
                transactionRefundRequestedEvent,
                transactionRefundErrorEvent,
                transactionRefundedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransactionRefunded =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundError =
            baseTransactionRefunded.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithUserReceiptV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(refundOperationId)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(refundOperationId)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in EXPIRED from NOTIFICATION_ERROR OK state outcome`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.EXPIRED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtilsV2.transactionUserReceiptAddErrorEvent(transactionUserReceiptData)
        val transactionExpired =
            TransactionTestUtilsV2.transactionExpiredEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedRequestedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedRequestedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError,
                transactionExpired
            )
                as List<TransactionEventV2<Any>>
        val baseTransactionExpired =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray()) as BaseTransactionExpiredV2
        val baseTransaction =
            baseTransactionExpired.transactionAtPreviousState
                as BaseTransactionWithRequestedUserReceiptV2

        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (transactionAuthorizationCompletedEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in EXPIRED_NOT_AUTHORIZED state`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.EXPIRED_NOT_AUTHORIZED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionExpiredEvent =
            TransactionTestUtilsV2.transactionExpiredEvent(
                TransactionTestUtilsV2.reduceEvents(transactionActivatedEvent)
            )
        val events =
            listOf(transactionActivatedEvent, transactionExpiredEvent)
                as List<TransactionEventV2<Any>>
        val baseTransaction = TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(1)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(1)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .authorizationRequestId(null)
                            .refundOperationId(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .authorizationRequestId(null)
                            .refundOperationId(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction v2 data into response searching by transaction id for transaction in REFUNDED FROM EXPIRED state outcome with first refund with no operation id`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val refundOperationId = "refundOperationId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.NPG
            )

        val transactionExpiredEvent =
            TransactionTestUtilsV2.transactionExpiredEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent
                )
            )
        val transactionRefundRequestedEvent =
            TransactionTestUtilsV2.transactionRefundRequestedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionExpiredEvent
                ),
                null // N.B.: Is null when getting error while retrieving authorization data from
                // gateway
            )
        val transactionRefundErrorEvent =
            TransactionTestUtilsV2.transactionRefundErrorEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionExpiredEvent,
                    transactionRefundRequestedEvent
                )
            )
        val transactionRefundRetryEvent =
            TransactionTestUtilsV2.transactionRefundRetriedEvent(
                1,
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionRefundedEvent =
            TransactionTestUtilsV2.transactionRefundedEvent(
                TransactionTestUtilsV2.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionExpiredEvent,
                    transactionRefundRequestedEvent,
                    transactionRefundErrorEvent,
                    transactionRefundRetryEvent
                ),
                NpgGatewayRefundData(refundOperationId)
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionExpiredEvent,
                transactionRefundRequestedEvent,
                transactionRefundErrorEvent,
                transactionRefundRetryEvent,
                transactionRefundedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransactionRefunded =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundError =
            baseTransactionRefunded.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequestedV2

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithRequestedAuthorizationV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .authenticationType("REGISTERED")
                            .notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationOperationId(
                                (transactionRefundRetryEvent.data
                                        .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(refundOperationId)
                            .authorizationCode(null)
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .authenticationType("REGISTERED")
                            .notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationOperationId(
                                (transactionRefundRetryEvent.data
                                    .transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(refundOperationId)
                            .authorizationCode(null)
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @ParameterizedTest
    @MethodSource("differentPaymentGatewayDataTestMethodSourceAuthOK")
    fun `should map successfully transaction v2 data into response searching by transaction id for NOTIFIED_OK transaction`(
        paymentGateway: TransactionAuthorizationRequestData.PaymentGateway,
        gatewayAuthRequestedData: TransactionGatewayAuthorizationRequestedData,
        gatewayAuthData: TransactionGatewayAuthorizationData,
        expectedBrand: String
    ) {
        val gatewayAuthorizationData = getGatewayAuthorizationData(gatewayAuthData)
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFIED_OK,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val authorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                paymentGateway,
                gatewayAuthRequestedData
            )
        val authorizedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(gatewayAuthData)
        val transactionClosureRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val closureSentEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.KO)
        val addUserReceiptEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val userReceiptAddErrorEvent =
            TransactionTestUtilsV2.transactionUserReceiptAddErrorEvent(addUserReceiptEvent.data)
        val userReceiptAddedEvent =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(userReceiptAddErrorEvent.data)
        val events =
            listOf(
                transactionActivatedEvent,
                authorizationRequestedEvent,
                authorizedEvent,
                transactionClosureRequestedEvent,
                closureSentEvent,
                addUserReceiptEvent,
                userReceiptAddErrorEvent,
                userReceiptAddedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as TransactionWithUserReceiptOkV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))

        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (authorizedEvent.data.transactionGatewayAuthorizationData
                                        as? NpgTransactionGatewayAuthorizationData)
                                    ?.operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(expectedBrand)
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(
                                gatewayAuthorizationData?.authorizationStatus
                            )
                            .gatewayErrorCode(gatewayAuthorizationData?.errorCode)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (authorizedEvent.data.transactionGatewayAuthorizationData
                                        as? NpgTransactionGatewayAuthorizationData)
                                    ?.operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(expectedBrand)
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(
                                gatewayAuthorizationData?.authorizationStatus
                            )
                            .gatewayErrorCode(gatewayAuthorizationData?.errorCode)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction data with 404 from PDV`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFIED_OK,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        val authorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent()
        val authorizedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedRequestedEvent =
            TransactionTestUtilsV2.transactionClosureRequestedEvent()
        val closureSentEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.KO)
        val addUserReceiptEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val userReceiptAddErrorEvent =
            TransactionTestUtilsV2.transactionUserReceiptAddErrorEvent(addUserReceiptEvent.data)
        val userReceiptAddedEvent =
            TransactionTestUtilsV2.transactionUserReceiptAddedEvent(userReceiptAddErrorEvent.data)
        val events =
            listOf(
                transactionActivatedEvent,
                authorizationRequestedEvent,
                authorizedEvent,
                transactionClosedRequestedEvent,
                closureSentEvent,
                addUserReceiptEvent,
                userReceiptAddErrorEvent,
                userReceiptAddedEvent
            )
                as List<TransactionEventV2<Any>>
        val baseTransaction =
            TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
                as TransactionWithUserReceiptOkV2
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events.take(3)))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.fromIterable(events.drop(3)))
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(
                Mono.error(
                    ConfidentialDataException(
                        WebClientResponseException(
                            HttpStatus.NOT_FOUND.value(),
                            "",
                            null,
                            null,
                            null
                        )
                    )
                )
            )
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail("N/A")
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (authorizedEvent.data.transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto().authenticationType("REGISTERED").notificationEmail("N/A")
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationOperationId(
                                (authorizedEvent.data.transactionGatewayAuthorizationData
                                        as NpgTransactionGatewayAuthorizationData)
                                    .operationId
                            )
                            .refundOperationId(null)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand("VISA")
                            .authorizationRequestId(
                                baseTransaction.transactionAuthorizationRequestData
                                    .authorizationRequestId
                            )
                            .paymentGateway(
                                baseTransaction.transactionAuthorizationRequestData.paymentGateway
                                    .toString()
                            )
                            .correlationId(UUID.fromString(correlationId))
                            .gatewayAuthorizationStatus(OperationResultDto.EXECUTED.value)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(
                        PspInfoDto()
                            .pspId(baseTransaction.transactionAuthorizationRequestData.pspId)
                            .businessName(
                                baseTransaction.transactionAuthorizationRequestData.pspBusinessName
                            )
                            .idChannel(
                                baseTransaction.transactionAuthorizationRequestData.pspChannelCode
                            )
                    )
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }

    @ParameterizedTest
    @MethodSource("registeredOrGuestTestMethodSource")
    fun `should map successfully transaction V2 data for not guest and registered user`(
        userId: String?,
        expectedAuthType: String
    ) {

        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val correlationId = UUID.randomUUID().toString()
        val orderId = "orderId"
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent =
            TransactionTestUtilsV2.transactionActivateEvent(
                NpgTransactionGatewayActivationData(orderId, correlationId)
            )
        transactionActivatedEvent.data.userId = userId
        val events = listOf(transactionActivatedEvent) as List<TransactionEventV2<Any>>
        val baseTransaction = TransactionTestUtilsV2.reduceEvents(*events.toTypedArray())
        given(transactionsViewRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionView.transactionId
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(transactionsViewHistoryRepository.findById(searchCriteria.transactionId))
            .willReturn(Mono.just(transactionView))
        given(
            transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                transactionView.transactionId
            )
        )
            .willReturn(Flux.empty())
        given(confidentialDataManager.decrypt(any<Confidential<Email>>(), any()))
            .willReturn(Mono.just(Email(TEST_EMAIL)))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .authenticationType(expectedAuthType)
                            .notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE),
                TransactionResultDto()
                    .userInfo(
                        UserInfoDto()
                            .authenticationType(expectedAuthType)
                            .notificationEmail(TEST_EMAIL)
                    )
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .events(convertEventsToEventInfoList(events))
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                            .correlationId(UUID.fromString(correlationId))
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .idTransaction(baseTransaction.transactionId.value())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .amount(it.transactionAmount.value)
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(it.companyName.value)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        SearchParamDecoderV2(
                            searchParameter = searchCriteria,
                            confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager),
                            confidentialFiscalCodeUtils =
                                ConfidentialFiscalCodeUtils(confidentialDataManager)
                        ),
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .consumeNextWith {
                assertEquals(expected, it)
                testedStatuses.add(
                    TransactionStatusDto.valueOf(it[0].transactionInfo.eventStatus.toString())
                )
            }
            .verifyComplete()
    }
}
