package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData as TransactionAuthorizationRequestDataV2
import it.pagopa.ecommerce.commons.documents.v2.TransactionClosureData as TransactionClosureDataV2
import it.pagopa.ecommerce.commons.documents.v2.TransactionEvent as TransactionEventV2
import it.pagopa.ecommerce.commons.documents.v2.TransactionUserReceiptData as TransactionUserReceiptDataV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionExpired as BaseTransactionExpiredV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithClosureError as BaseTransactionWithClosureErrorV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithCompletedAuthorization as BaseTransactionWithCompletedAuthorizationV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRefundRequested as BaseTransactionWithRefundRequestedV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRequestedUserReceipt as BaseTransactionWithRequestedUserReceiptV2
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithUserReceipt as BaseTransactionWithUserReceiptV2
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils as TransactionTestUtilsV2
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.ZonedDateTime
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceForTransactionV2DataProviderTest {

    companion object {
        val testedStatuses: MutableSet<TransactionStatusDto> = HashSet()

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
    }

    private val transactionsViewRepository: TransactionsViewRepository = mock()

    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()

    private val ecommerceTransactionDataProvider =
        EcommerceTransactionDataProvider(
            transactionsViewRepository,
            transactionsEventStoreRepository
        )

    @Test
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in ACTIVATED state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
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
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
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
    fun `should map successfully transaction V2 data into response searching by transaction id for transaction in AUTHORIZATION_COMPLETED KO state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.AUTHORIZATION_COMPLETED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.FAILED
                )
            )
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Rifiutato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.KO)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CLOSURE_ERROR,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val closureErrorEvent = TransactionTestUtilsV2.transactionClosureErrorEvent()
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CANCELLATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CANCELLATION_EXPIRED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.CANCELED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.UNAUTHORIZED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.FAILED
                )
            )
        val transactionCloseFailedEvent =
            TransactionTestUtilsV2.transactionClosureFailedEvent(
                TransactionClosureDataV2.Outcome.KO
            )

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Rifiutato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
        val transactionClosedEvent =
            TransactionTestUtilsV2.transactionClosedEvent(TransactionClosureDataV2.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtilsV2.transactionUserReceiptRequestedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFICATION_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.NOTIFIED_KO,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUND_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
            )

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUND_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
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
                    transactionRefundRequestedEvent
                )
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
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
                    transactionRefundRequestedEvent,
                    transactionRefundErrorEvent
                )
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.KO
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError,
                    transactionExpiredEvent
                )
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
                )
            )
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.EXPIRED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtilsV2.transactionUserReceiptData(
                TransactionUserReceiptDataV2.Outcome.OK
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtilsV2.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestDataV2.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtilsV2.transactionAuthorizationCompletedEvent(
                TransactionTestUtilsV2.npgTransactionGatewayAuthorizationData(
                    OperationResultDto.EXECUTED
                )
            )
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = baseTransaction.transactionAuthorizationRequestData.fee
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Confermato")
                            .statusDetails("authorizationCode")
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(baseTransaction.transactionAuthorizationCompletedData.rrn)
                            .authorizationCode(
                                baseTransaction.transactionAuthorizationCompletedData
                                    .authorizationCode
                            )
                            .paymentMethodName(
                                baseTransaction.transactionAuthorizationRequestData
                                    .paymentMethodName
                            )
                            .brand(
                                baseTransaction.transactionAuthorizationRequestData.brand!!
                                    .toString()
                            )
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(
                                            baseTransaction.transactionUserReceiptData
                                                .receivingOfficeName
                                        )
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
                    searchParams = searchCriteria,
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtilsV2.transactionDocument(
                TransactionStatusDto.EXPIRED_NOT_AUTHORIZED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtilsV2.transactionActivateEvent()
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
            .willReturn(Flux.fromIterable(events))
        val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
        val fee = 0
        val totalAmount = amount.plus(fee)
        val expected =
            listOf(
                TransactionResultDto()
                    .userInfo(UserInfoDto().authenticationType("GUEST").notificationEmail(""))
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
                            .status("Cancellato")
                            .statusDetails(null)
                            .eventStatus(
                                it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
                                    .valueOf(transactionView.status.toString())
                            )
                            .amount(amount)
                            .fee(fee)
                            .grandTotal(totalAmount)
                            .rrn(null)
                            .authorizationCode(null)
                            .paymentMethodName(null)
                            .brand(null)
                    )
                    .paymentInfo(
                        PaymentInfoDto()
                            .origin(baseTransaction.clientId.toString())
                            .details(
                                baseTransaction.paymentNotices.map {
                                    PaymentDetailInfoDto()
                                        .subject(it.transactionDescription.value)
                                        .rptId(it.rptId.value)
                                        .idTransaction(baseTransaction.transactionId.value())
                                        .paymentToken(it.paymentToken.value)
                                        .creditorInstitution(null)
                                        .paFiscalCode(it.transferList[0].paFiscalCode)
                                }
                            )
                    )
                    .pspInfo(PspInfoDto().pspId(null).businessName(null).idChannel(null))
                    .product(ProductDto.ECOMMERCE)
            )
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
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
