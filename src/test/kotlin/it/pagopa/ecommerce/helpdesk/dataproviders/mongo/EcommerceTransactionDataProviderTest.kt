package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v1.TransactionClosureData
import it.pagopa.ecommerce.commons.documents.v1.TransactionEvent
import it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptData
import it.pagopa.ecommerce.commons.domain.v1.TransactionWithUserReceiptOk
import it.pagopa.ecommerce.commons.domain.v1.pojos.*
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
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

class EcommerceTransactionDataProviderTest {

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
    fun `should count total transactions by rptId successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        given(transactionsViewRepository.countTransactionsWithRptId(searchCriteria.rptId))
            .willReturn(Mono.just(2))
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectNext(2)
            .verifyComplete()
    }

    @Test
    fun `should count total transactions by paymentToken successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
        given(
                transactionsViewRepository.countTransactionsWithPaymentToken(
                    searchCriteria.paymentToken
                )
            )
            .willReturn(Mono.just(2))
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectNext(2)
            .verifyComplete()
    }

    @Test
    fun `should count total transactions by transactionId successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        given(transactionsViewRepository.existsById(searchCriteria.transactionId))
            .willReturn(Mono.just(true))
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectNext(1)
            .verifyComplete()
    }

    @Test
    fun `should handle no transactions found by rptId successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        given(transactionsViewRepository.countTransactionsWithRptId(searchCriteria.rptId))
            .willReturn(Mono.just(0))
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `should handle no transaction found by paymentToken successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
        given(
                transactionsViewRepository.countTransactionsWithPaymentToken(
                    searchCriteria.paymentToken
                )
            )
            .willReturn(Mono.just(0))
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `should handle no transaction found by transactionId successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        given(transactionsViewRepository.existsById(searchCriteria.transactionId))
            .willReturn(Mono.just(false))
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectNext(0)
            .verifyComplete()
    }

    @Test
    fun `should return error for search by email as invalid search criteria`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should return error for search by user fiscal code as invalid search criteria`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserFiscalCode("fiscal code")
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should return error for search by unknown search criteria`() {
        val searchCriteria: HelpDeskSearchTransactionRequestDto = mock()
        given(searchCriteria.type).willReturn("UNKNOWN")
        StepVerifier.create(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should map successfully transaction data into response searching by rptId for NOTIFIED_OK transaction`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFIED_OK,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.OK)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val authorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent()
        val authorizedEvent = TransactionTestUtils.transactionAuthorizationCompletedEvent()
        val closureSentEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.KO)
        val addUserReceiptEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val userReceiptAddErrorEvent =
            TransactionTestUtils.transactionUserReceiptAddErrorEvent(addUserReceiptEvent.data)
        val userReceiptAddedEvent =
            TransactionTestUtils.transactionUserReceiptAddedEvent(userReceiptAddErrorEvent.data)
        val events =
            listOf(
                transactionActivatedEvent,
                authorizationRequestedEvent,
                authorizedEvent,
                closureSentEvent,
                addUserReceiptEvent,
                userReceiptAddErrorEvent,
                userReceiptAddedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as TransactionWithUserReceiptOk
        given(
                transactionsViewRepository
                    .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                        rptId = searchCriteria.rptId,
                        skip = pageSize * pageNumber,
                        limit = pageSize
                    )
            )
            .willReturn(Flux.just(transactionView))
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
                            .statusDetails(null)
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
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction data into response searching by paymentToken for NOTIFIED_OK transaction`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFIED_OK,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.OK)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val authorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent()
        val authorizedEvent = TransactionTestUtils.transactionAuthorizationCompletedEvent()
        val closureSentEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.KO)
        val addUserReceiptEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val userReceiptAddErrorEvent =
            TransactionTestUtils.transactionUserReceiptAddErrorEvent(addUserReceiptEvent.data)
        val userReceiptAddedEvent =
            TransactionTestUtils.transactionUserReceiptAddedEvent(userReceiptAddErrorEvent.data)
        val events =
            listOf(
                transactionActivatedEvent,
                authorizationRequestedEvent,
                authorizedEvent,
                closureSentEvent,
                addUserReceiptEvent,
                userReceiptAddErrorEvent,
                userReceiptAddedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as TransactionWithUserReceiptOk
        given(
                transactionsViewRepository
                    .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                        paymentToken = searchCriteria.paymentToken,
                        skip = pageSize * pageNumber,
                        limit = pageSize
                    )
            )
            .willReturn(Flux.just(transactionView))
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
                            .statusDetails(null)
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
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun `should map successfully transaction data into response searching by transaction id for NOTIFIED_OK transaction`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFIED_OK,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.OK)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val authorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent()
        val authorizedEvent = TransactionTestUtils.transactionAuthorizationCompletedEvent()
        val closureSentEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.KO)
        val addUserReceiptEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val userReceiptAddErrorEvent =
            TransactionTestUtils.transactionUserReceiptAddErrorEvent(addUserReceiptEvent.data)
        val userReceiptAddedEvent =
            TransactionTestUtils.transactionUserReceiptAddedEvent(userReceiptAddErrorEvent.data)
        val events =
            listOf(
                transactionActivatedEvent,
                authorizationRequestedEvent,
                authorizedEvent,
                closureSentEvent,
                addUserReceiptEvent,
                userReceiptAddErrorEvent,
                userReceiptAddedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as TransactionWithUserReceiptOk
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
                            .statusDetails(null)
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
    fun `should return error for invalid search by user fiscal code`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserFiscalCode("fiscal code")
        val pageSize = 100
        val pageNumber = 0

        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should return error for invalid search by user email`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("email@email.it")
        val pageSize = 100
        val pageNumber = 0

        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should return error for invalid search by unknown search criteria`() {
        val searchCriteria: HelpDeskSearchTransactionRequestDto = mock()
        val pageSize = 100
        val pageNumber = 0
        given(searchCriteria.type).willReturn("UNKNOWN")
        StepVerifier.create(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
                    skip = pageSize,
                    limit = pageNumber
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should map successfully transaction data into response searching by transaction id for transaction in ACTIVATED state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val events = listOf(transactionActivatedEvent) as List<TransactionEvent<Any>>
        val baseTransaction = TransactionTestUtils.reduceEvents(*events.toTypedArray())
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in AUTHORIZATION_REQUESTED state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.AUTHORIZATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val events =
            listOf(transactionActivatedEvent, transactionAuthorizationRequestedEvent)
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedAuthorization
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in AUTHORIZATION_COMPLETED OK state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.AUTHORIZATION_COMPLETED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorization
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in AUTHORIZATION_COMPLETED KO state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.AUTHORIZATION_COMPLETED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.KO)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorization
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in CLOSED state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.CLOSED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorization
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in CLOSED state outcome KO`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.CLOSED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.KO)
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorization
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in CLOSURE_ERROR state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.CLOSURE_ERROR,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val closureErrorEvent = TransactionTestUtils.transactionClosureErrorEvent()
        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                closureErrorEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            (TransactionTestUtils.reduceEvents(*events.toTypedArray())
                    as BaseTransactionWithClosureError)
                .transactionAtPreviousState as BaseTransactionWithCompletedAuthorization
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in CANCELLATION_REQUESTED state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.CANCELLATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionCancellationRequestedEvent =
            TransactionTestUtils.transactionUserCanceledEvent()
        val events =
            listOf(transactionActivatedEvent, transactionCancellationRequestedEvent)
                as List<TransactionEvent<Any>>
        val baseTransaction = (TransactionTestUtils.reduceEvents(*events.toTypedArray()))
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in CANCELLATION_EXPIRED state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.CANCELLATION_EXPIRED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionCancellationRequestedEvent =
            TransactionTestUtils.transactionUserCanceledEvent()
        val transactionExpiredEvent =
            TransactionTestUtils.transactionExpiredEvent(
                TransactionTestUtils.reduceEvents(
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
                as List<TransactionEvent<Any>>
        val baseTransaction = (TransactionTestUtils.reduceEvents(*events.toTypedArray()))
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in CANCELED state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.CANCELED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionCancellationRequestedEvent =
            TransactionTestUtils.transactionUserCanceledEvent()
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionCancellationRequestedEvent,
                transactionClosedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction = (TransactionTestUtils.reduceEvents(*events.toTypedArray()))
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in UNAUTHORIZED state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.UNAUTHORIZED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.KO)
        val transactionCloseFailedEvent =
            TransactionTestUtils.transactionClosureFailedEvent(TransactionClosureData.Outcome.KO)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionCloseFailedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithCompletedAuthorization

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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in NOTIFICATION_REQUESTED OK state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFICATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.OK)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFICATION_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in NOTIFICATION_ERROR OK state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFICATION_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.OK)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddErrorEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in NOTIFICATION_ERROR KO state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFICATION_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddErrorEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in NOTIFIED_KO state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.NOTIFIED_KO,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddedEvent(transactionUserReceiptData)

        val events =
            listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionAuthorizationCompletedEvent,
                transactionClosedEvent,
                transactionNotificationRequestedEvent,
                transactionUserReceiptError
            )
                as List<TransactionEvent<Any>>
        val baseTransaction =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.REFUND_REQUESTED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionRefundRequestedEvent =
            TransactionTestUtils.transactionRefundRequestedEvent(
                TransactionTestUtils.reduceEvents(
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
                as List<TransactionEvent<Any>>
        val baseTransaction =
            (TransactionTestUtils.reduceEvents(*events.toTypedArray())
                    as BaseTransactionWithRefundRequested)
                .transactionAtPreviousState as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in REFUND_ERROR state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.REFUND_ERROR,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionRefundRequestedEvent =
            TransactionTestUtils.transactionRefundRequestedEvent(
                TransactionTestUtils.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
            )
        val transactionRefundErrorEvent =
            TransactionTestUtils.transactionRefundErrorEvent(
                TransactionTestUtils.reduceEvents(
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
                as List<TransactionEvent<Any>>
        val baseTransactionRefundError =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequested

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequested

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithUserReceipt
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in REFUNDED state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionRefundRequestedEvent =
            TransactionTestUtils.transactionRefundRequestedEvent(
                TransactionTestUtils.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
            )
        val transactionRefundErrorEvent =
            TransactionTestUtils.transactionRefundErrorEvent(
                TransactionTestUtils.reduceEvents(
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
            TransactionTestUtils.transactionRefundedEvent(
                TransactionTestUtils.reduceEvents(
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
                as List<TransactionEvent<Any>>
        val baseTransactionRefunded =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequested

        val baseTransactionRefundError =
            baseTransactionRefunded.transactionAtPreviousState as BaseTransactionWithRefundRequested

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequested

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithUserReceipt
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in REFUNDED FROM EXPIRED state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.REFUNDED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.KO)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddedEvent(transactionUserReceiptData)
        val transactionExpiredEvent =
            TransactionTestUtils.transactionExpiredEvent(
                TransactionTestUtils.reduceEvents(
                    transactionActivatedEvent,
                    transactionAuthorizationRequestedEvent,
                    transactionAuthorizationCompletedEvent,
                    transactionClosedEvent,
                    transactionNotificationRequestedEvent,
                    transactionUserReceiptError
                )
            )
        val transactionRefundRequestedEvent =
            TransactionTestUtils.transactionRefundRequestedEvent(
                TransactionTestUtils.reduceEvents(
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
            TransactionTestUtils.transactionRefundErrorEvent(
                TransactionTestUtils.reduceEvents(
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
            TransactionTestUtils.transactionRefundedEvent(
                TransactionTestUtils.reduceEvents(
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
                as List<TransactionEvent<Any>>
        val baseTransactionRefunded =
            TransactionTestUtils.reduceEvents(*events.toTypedArray())
                as BaseTransactionWithRefundRequested

        val baseTransactionRefundError =
            baseTransactionRefunded.transactionAtPreviousState as BaseTransactionWithRefundRequested

        val baseTransactionRefundRequested =
            baseTransactionRefundError.transactionAtPreviousState
                as BaseTransactionWithRefundRequested

        val baseTransaction =
            baseTransactionRefundRequested.transactionAtPreviousState
                as BaseTransactionWithUserReceipt
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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in EXPIRED from NOTIFICATION_ERROR OK state outcome`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.EXPIRED,
                ZonedDateTime.now()
            )
        val transactionUserReceiptData =
            TransactionTestUtils.transactionUserReceiptData(TransactionUserReceiptData.Outcome.OK)
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionAuthorizationRequestedEvent =
            TransactionTestUtils.transactionAuthorizationRequestedEvent(
                TransactionAuthorizationRequestData.PaymentGateway.XPAY
            )
        val transactionAuthorizationCompletedEvent =
            TransactionTestUtils.transactionAuthorizationCompletedEvent(AuthorizationResultDto.OK)
        val transactionClosedEvent =
            TransactionTestUtils.transactionClosedEvent(TransactionClosureData.Outcome.OK)
        val transactionNotificationRequestedEvent =
            TransactionTestUtils.transactionUserReceiptRequestedEvent(transactionUserReceiptData)
        val transactionUserReceiptError =
            TransactionTestUtils.transactionUserReceiptAddErrorEvent(transactionUserReceiptData)
        val transactionExpired =
            TransactionTestUtils.transactionExpiredEvent(
                TransactionTestUtils.reduceEvents(
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
                as List<TransactionEvent<Any>>
        val baseTransactionExpired =
            TransactionTestUtils.reduceEvents(*events.toTypedArray()) as BaseTransactionExpired
        val baseTransaction =
            baseTransactionExpired.transactionAtPreviousState
                as BaseTransactionWithRequestedUserReceipt

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
                            .statusDetails(null)
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
    fun `should map successfully transaction data into response searching by transaction id for transaction in EXPIRED_NOT_AUTHORIZED state`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val pageSize = 100
        val pageNumber = 0
        val transactionView =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.EXPIRED_NOT_AUTHORIZED,
                ZonedDateTime.now()
            )
        val transactionActivatedEvent = TransactionTestUtils.transactionActivateEvent()
        val transactionExpiredEvent =
            TransactionTestUtils.transactionExpiredEvent(
                TransactionTestUtils.reduceEvents(transactionActivatedEvent)
            )
        val events =
            listOf(transactionActivatedEvent, transactionExpiredEvent)
                as List<TransactionEvent<Any>>
        val baseTransaction = TransactionTestUtils.reduceEvents(*events.toTypedArray())
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
