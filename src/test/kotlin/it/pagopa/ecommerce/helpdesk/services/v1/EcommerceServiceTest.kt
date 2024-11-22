package it.pagopa.ecommerce.helpdesk.services.v1

import io.vavr.control.Either
import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.documents.v2.TransactionActivatedData
import it.pagopa.ecommerce.commons.documents.v2.TransactionActivatedEvent
import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.domain.v2.TransactionActivated
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRequestedAuthorization
import it.pagopa.ecommerce.commons.exceptions.NpgResponseException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationDto
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationTypeDto
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.AUTHORIZATION_CODE
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.AUTHORIZATION_REQUEST_ID
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.PSP_ID
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.TRANSACTION_ID
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadGatewayException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadRequestException
import it.pagopa.ecommerce.helpdesk.utils.TransactionInfoUtils
import it.pagopa.ecommerce.helpdesk.utils.TransactionInfoUtils.Companion.buildOrderResponseDtoNullOperation
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchDateTimeRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchDeadLetterEventsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceServiceTest {

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()
    private val deadLetterDataProvider: DeadLetterDataProvider = mock()
    private val confidentialDataManager: ConfidentialDataManager = mock()
    private val testEmail = "test@test.it"
    private val encryptedEmail = TransactionTestUtils.EMAIL.opaqueData
    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()
    private val npgApiKeyConfiguration: NpgApiKeyConfiguration = mock()
    private val npgClient: NpgClient = mock()

    private val ecommerceService =
        EcommerceService(
            ecommerceTransactionDataProvider,
            deadLetterDataProvider,
            confidentialDataManager,
            npgClient = npgClient,
            npgApiKeyConfiguration = npgApiKeyConfiguration
        )

    @Test
    fun `should return found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactions =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat { this.searchParameter == searchCriteria }
                )
            )
            .willReturn(Mono.just(totalCount))
        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = argThat { this.searchParameter == searchCriteria },
                    skip = eq(pageSize * pageNumber),
                    limit = eq(pageSize)
                )
            )
            .willReturn(Mono.just(transactions))
        val expectedResponse =
            SearchTransactionResponseDto()
                .transactions(transactions)
                .page(PageInfoDto().results(transactions.size).total(10).current(pageNumber))
        StepVerifier.create(
                ecommerceService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()

        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(any())
        verify(ecommerceTransactionDataProvider, times(1)).findResult(any(), any(), any())
    }

    @Test
    fun `should return error for no transaction found performing only count query`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 0
        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat { this.searchParameter == searchCriteria }
                )
            )
            .willReturn(Mono.just(totalCount))
        StepVerifier.create(
                ecommerceService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = searchCriteria
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()

        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(any())
        verify(ecommerceTransactionDataProvider, times(0)).findResult(any(), any(), any())
    }

    @Test
    fun `Should return dead letter event for no input time range`() {
        val request =
            EcommerceSearchDeadLetterEventsRequestDto().source(DeadLetterSearchEventSourceDto.ALL)
        val pageNumber = 0
        val pageSize = 10
        val deadLetterEventList =
            listOf(
                DeadLetterEventDto()
                    .queueName("queueName1")
                    .data("data1")
                    .timestamp(OffsetDateTime.MIN),
                DeadLetterEventDto()
                    .queueName("queueName2")
                    .data("data2")
                    .timestamp(OffsetDateTime.MIN)
            )
        val expectedResponse =
            SearchDeadLetterEventResponseDto()
                .deadLetterEvents(deadLetterEventList)
                .page(PageInfoDto().current(0).results(deadLetterEventList.size).total(1))
        given(deadLetterDataProvider.totalRecordCount(request))
            .willReturn(mono { deadLetterEventList.size })
        given(deadLetterDataProvider.findResult(request, 0, 10))
            .willReturn(mono { deadLetterEventList })
        StepVerifier.create(
                ecommerceService.searchDeadLetterEvents(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchRequest = request
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()
    }

    @Test
    fun `Should return dead letter event with time range filter`() {
        val request =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
        val pageNumber = 0
        val pageSize = 10
        val deadLetterEventList =
            listOf(
                DeadLetterEventDto()
                    .queueName("queueName1")
                    .data("data1")
                    .timestamp(OffsetDateTime.MIN),
                DeadLetterEventDto()
                    .queueName("queueName2")
                    .data("data2")
                    .timestamp(OffsetDateTime.MIN)
            )
        val expectedResponse =
            SearchDeadLetterEventResponseDto()
                .deadLetterEvents(deadLetterEventList)
                .page(PageInfoDto().current(0).results(deadLetterEventList.size).total(1))
        given(deadLetterDataProvider.totalRecordCount(request))
            .willReturn(mono { deadLetterEventList.size })
        given(deadLetterDataProvider.findResult(request, 0, 10))
            .willReturn(mono { deadLetterEventList })
        StepVerifier.create(
                ecommerceService.searchDeadLetterEvents(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchRequest = request
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()
    }

    @Test
    fun `Should return error for invalid time range filter`() {
        val request =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MAX)
                        .endDate(OffsetDateTime.MIN)
                )
        val pageNumber = 0
        val pageSize = 10
        val deadLetterEventList =
            listOf(
                DeadLetterEventDto()
                    .queueName("queueName1")
                    .data("data1")
                    .timestamp(OffsetDateTime.MIN),
                DeadLetterEventDto()
                    .queueName("queueName2")
                    .data("data2")
                    .timestamp(OffsetDateTime.MIN)
            )
        given(deadLetterDataProvider.totalRecordCount(request))
            .willReturn(mono { deadLetterEventList.size })
        given(deadLetterDataProvider.findResult(request, 0, 10))
            .willReturn(mono { deadLetterEventList })
        StepVerifier.create(
                ecommerceService.searchDeadLetterEvents(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchRequest = request
                )
            )
            .expectError(InvalidSearchCriteriaException::class.java)
            .verify()
    }

    @Test
    fun `should invoke PDV only once recovering found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactionDocument =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            ) as BaseTransactionView
        val transactions =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(confidentialDataManager.encrypt(Email(testEmail)))
            .willReturn(Mono.just(Confidential(encryptedEmail)))
        given(transactionsViewRepository.countTransactionsWithEmail(encryptedEmail))
            .willReturn(Mono.just(totalCount.toLong()))
        given(
                transactionsViewRepository
                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                        encryptedEmail = any(),
                        skip = any(),
                        limit = any()
                    )
            )
            .willReturn(Flux.just(transactionDocument))
        given(transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(any()))
            .willReturn(
                Flux.just(
                    TransactionTestUtils.transactionActivateEvent() as BaseTransactionEvent<Any>
                )
            )
        val ecommerceServiceLocalMock =
            EcommerceService(
                confidentialDataManager = confidentialDataManager,
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                deadLetterDataProvider = deadLetterDataProvider,
                npgClient = npgClient,
                npgApiKeyConfiguration = npgApiKeyConfiguration
            )

        StepVerifier.create(
                ecommerceServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext {
                Assertions.assertEquals(
                    it.page,
                    PageInfoDto().results(transactions.size).total(10).current(pageNumber)
                )
            }
            .verifyComplete()

        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    companion object {
        private val correlationId = UUID.randomUUID().toString()
        private const val OPERATION_ID = "operationId"
    }

    @Test
    fun `Should successfully retrieve NPG operations`() {
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)

        val transactionEvent = mock<TransactionActivatedEvent>()
        val transaction = mock<TransactionActivated>()
        val transactionData = mock<TransactionActivatedData>()

        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))

        given(transactionEvent.data).willReturn(transactionData)
        given(transaction.transactionActivatedData).willReturn(transactionData)

        val npgOperation =
            OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDto.AUTHORIZATION
                operationResult =
                    it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto.EXECUTED
                additionalData = mapOf("authorizationCode" to "auth123", "rrn" to "rrn123")
            }
        val orderResponse = OrderResponseDto().apply { operations = listOf(npgOperation) }

        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.operationResult == OperationResultDto.EXECUTED
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle missing operation data`() {
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val transactionEvent =
            mock<BaseTransactionEvent<BaseTransactionWithRequestedAuthorization>>()
        val baseTransaction = mock<BaseTransactionWithRequestedAuthorization>()
        val authRequestData = mock<TransactionAuthorizationRequestData>()

        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))

        given(transactionEvent.data).willReturn(baseTransaction)
        given(baseTransaction.transactionAuthorizationRequestData).willReturn(authRequestData)
        given(authRequestData.paymentMethodName).willReturn("CARDS")
        given(authRequestData.pspId).willReturn("pspId")
        given(npgApiKeyConfiguration[any<NpgClient.PaymentMethod>(), any()])
            .willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), any(), any()))
            .willReturn(buildOrderResponseDtoNullOperation())

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNext(SearchNpgOperationsResponseDto().apply { operations = null })
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG server error`() {
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[any<NpgClient.PaymentMethod>(), any()])
            .willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), any(), any()))
            .willReturn(
                Mono.error(
                    NpgResponseException(
                        "error",
                        emptyList(),
                        Optional.of(HttpStatus.INTERNAL_SERVER_ERROR),
                        RuntimeException()
                    )
                )
            )

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectError(NpgBadGatewayException::class.java)
            .verify()
    }

    @Test
    fun `Should handle NPG client error`() {
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[any<NpgClient.PaymentMethod>(), any()])
            .willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), any(), any()))
            .willReturn(
                Mono.error(
                    NpgResponseException(
                        "error",
                        emptyList(),
                        Optional.of(HttpStatus.BAD_REQUEST),
                        RuntimeException()
                    )
                )
            )

        StepVerifier.create(ecommerceService.searchNpgOperations(transactionId))
            .expectError(NpgBadRequestException::class.java)
            .verify()
    }

    @Test
    fun `Should handle NPG operations with no operations in order response`() {
        val transactionId = TRANSACTION_ID
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        val orderResponse = OrderResponseDto().apply { operations = null }

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNext(SearchNpgOperationsResponseDto().apply { operations = null })
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operations with no RRN in additional data`() {
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        val npgOperation =
            OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDto.AUTHORIZATION
                operationResult =
                    it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto.EXECUTED
                additionalData = mapOf("authorizationCode" to AUTHORIZATION_CODE)
            }

        val orderResponse = OrderResponseDto().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.additionalData?.rrn == null
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operations with no payment method`() {
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)

        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        val npgOperation =
            OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDto.AUTHORIZATION
                operationResult =
                    it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto.EXECUTED
                this.paymentMethod = null
            }

        val orderResponse = OrderResponseDto().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.paymentMethod == null
            }
            .verifyComplete()
    }
}
