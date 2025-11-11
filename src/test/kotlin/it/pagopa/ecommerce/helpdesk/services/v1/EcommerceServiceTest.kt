package it.pagopa.ecommerce.helpdesk.services.v1

import io.vavr.control.Either
import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.client.NpgClient.PaymentMethod
import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.v1.Email
import it.pagopa.ecommerce.commons.exceptions.NpgResponseException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationDto as OperationDtoV1
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto as OperationResultDtoV1
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationTypeDto as OperationTypeDtoV1
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto as OrderResponseDtoV1
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.AUTHORIZATION_REQUEST_ID
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.PSP_ID
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.TRANSACTION_ID
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreHistoryRepository as TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewHistoryRepository as TransactionsViewHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadGatewayException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadRequestException
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchDateTimeRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchDeadLetterEventsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationResultDto as OperationResultModelV1
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.PspInfoDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionResultDto
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*
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
    private val ecommerceTransactionDataProviderV2:
        it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider =
        mock()
    private val deadLetterDataProvider: DeadLetterDataProvider = mock()
    private val confidentialDataManager: ConfidentialDataManager = mock()
    private val testEmail = "test@test.it"
    private val encryptedEmail = TransactionTestUtils.EMAIL.opaqueData
    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsViewHistoryRepository: TransactionsViewHistoryRepository = mock()
    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()
    private val transactionsEventStoreHistoryRepository:
        TransactionsEventStoreHistoryRepository<Any> =
        mock()
    private val npgApiKeyConfiguration: NpgApiKeyConfiguration = mock()
    private val npgClient: NpgClient = mock()

    private val ecommerceService =
        EcommerceService(
            ecommerceTransactionDataProvider,
            ecommerceTransactionDataProviderV2,
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
        given(transactionsViewHistoryRepository.countTransactionsWithEmail(encryptedEmail))
            .willReturn(Mono.just(0))
        given(
                transactionsViewRepository
                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                        encryptedEmail = any(),
                        skip = any(),
                        limit = any()
                    )
            )
            .willReturn(Flux.just(transactionDocument))
        given(
                transactionsViewHistoryRepository
                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                        encryptedEmail = any(),
                        skip = any(),
                        limit = any()
                    )
            )
            .willReturn(Flux.empty())
        given(transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(any()))
            .willReturn(
                Flux.just(
                    TransactionTestUtils.transactionActivateEvent() as BaseTransactionEvent<Any>
                )
            )
        given(transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(any()))
            .willReturn(
                Flux.empty()
            )
        val ecommerceServiceLocalMock =
            EcommerceService(
                confidentialDataManager = confidentialDataManager,
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsViewHistoryRepository = transactionsViewHistoryRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository,
                        transactionsEventStoreHistoryRepository =
                            transactionsEventStoreHistoryRepository
                    ),
                ecommerceTransactionDataProviderV2 =
                    it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo
                        .EcommerceTransactionDataProvider(
                            transactionsViewRepository = transactionsViewRepository,
                            transactionsViewHistoryRepository = transactionsViewHistoryRepository,
                            transactionsEventStoreRepository = transactionsEventStoreRepository,
                            transactionsEventStoreHistoryRepository =
                                transactionsEventStoreHistoryRepository
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
        private val correlationId = UUID.randomUUID()
        private const val OPERATION_ID = "operationId"
    }

    @Test
    fun `Should successfully map NPG operation response with all fields`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = PaymentMethod.CARDS

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethod.serviceName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        val npgOperation =
            OperationDtoV1().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDtoV1.AUTHORIZATION
                operationResult = OperationResultDtoV1.EXECUTED
                additionalData = mapOf("authorizationCode" to "auth123", "rrn" to "rrn123")
            }
        val orderResponse = OrderResponseDtoV1().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))

        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("test-api-key"))
        given(npgClient.getOrder(any(), eq("test-api-key"), eq(authorizationRequestId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.operationResult == OperationResultModelV1.EXECUTED
            }
            .verifyComplete()

        verify(npgClient)
            .getOrder(eq(correlationId), eq("test-api-key"), eq(authorizationRequestId))
    }

    @Test
    fun `Should handle NPG operation with missing additionalData`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = PaymentMethod.CARDS

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethod.serviceName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        val npgOperation =
            OperationDtoV1().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDtoV1.AUTHORIZATION
                operationResult = OperationResultDtoV1.EXECUTED
                additionalData = null
            }
        val orderResponse = OrderResponseDtoV1().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))
        given(npgApiKeyConfiguration[PaymentMethod.CARDS, pspId])
            .willReturn(Either.right("test-api-key"))
        given(npgClient.getOrder(any(), any(), any())).willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.first()?.additionalData?.authorizationCode == null &&
                    response.operations?.first()?.additionalData?.rrn == null
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operation with missing authorization code`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = PaymentMethod.CARDS

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethod.serviceName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        val npgOperation =
            OperationDtoV1().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDtoV1.AUTHORIZATION
                operationResult = OperationResultDtoV1.EXECUTED
                additionalData = mapOf("rrn" to "rrn123")
            }
        val orderResponse = OrderResponseDtoV1().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))
        given(npgApiKeyConfiguration[PaymentMethod.CARDS, pspId])
            .willReturn(Either.right("test-api-key"))
        given(npgClient.getOrder(any(), any(), any())).willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.first()?.additionalData?.authorizationCode == null &&
                    response.operations?.first()?.additionalData?.rrn == "rrn123"
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operation with missing rrn`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = PaymentMethod.CARDS

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethod.serviceName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        val npgOperation =
            OperationDtoV1().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDtoV1.AUTHORIZATION
                operationResult = OperationResultDtoV1.EXECUTED
                additionalData = mapOf("authorizationCode" to "auth123")
            }
        val orderResponse = OrderResponseDtoV1().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))
        given(npgApiKeyConfiguration[PaymentMethod.CARDS, pspId])
            .willReturn(Either.right("test-api-key"))
        given(npgClient.getOrder(any(), any(), any())).willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.first()?.additionalData?.authorizationCode == "auth123" &&
                    response.operations?.first()?.additionalData?.rrn == null
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG response with null operations list`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val paymentMethodName = "CARDS"
        val pspId = PSP_ID

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethodName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        val orderResponse = OrderResponseDtoV1().apply { operations = null }

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))
        given(npgApiKeyConfiguration[PaymentMethod.CARDS, pspId])
            .willReturn(Either.right("test-api-key"))
        given(npgClient.getOrder(any(), any(), any())).willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response -> response.operations == null }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operation with null paymentMethod`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val paymentMethodName = "CARDS"
        val pspId = PSP_ID

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethodName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        val npgOperation =
            it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDtoV1.AUTHORIZATION
                operationResult = OperationResultDtoV1.EXECUTED
                paymentMethod = null
            }

        val orderResponse = OrderResponseDtoV1().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))
        given(npgApiKeyConfiguration[PaymentMethod.CARDS, pspId])
            .willReturn(Either.right("test-api-key"))
        given(npgClient.getOrder(any(), any(), any())).willReturn(Mono.just(orderResponse))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response -> response.operations?.first()?.paymentMethod == null }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG server error`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = PaymentMethod.CARDS

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethod.serviceName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))

        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("test-api-key"))

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

        verify(npgClient)
            .getOrder(eq(correlationId), eq("test-api-key"), eq(authorizationRequestId))
    }

    @Test
    fun `Should handle NPG client error`() {
        val authorizationRequestId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = PaymentMethod.CARDS

        val transactionInfo =
            TransactionInfoDto()
                .correlationId(correlationId)
                .authorizationRequestId(authorizationRequestId)
                .paymentMethodName(paymentMethod.serviceName)

        val pspInfo = PspInfoDto().pspId(pspId)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))

        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("test-api-key"))

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

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectError(NpgBadRequestException::class.java)
            .verify()

        verify(npgClient)
            .getOrder(eq(correlationId), eq("test-api-key"), eq(authorizationRequestId))
    }

    @Test
    fun `Should throw NoResultFoundException when findResult returns empty list`() {
        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(emptyList()))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectError(NoResultFoundException::class.java)
            .verify()
    }

    @Test
    fun `Should throw NoResultFoundException when transaction info is null`() {
        val transactionResultDto = TransactionResultDto()

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectError(NoResultFoundException::class.java)
            .verify()
    }

    @Test
    fun `Should throw NoResultFoundException when correlationId is null`() {
        val transactionInfo =
            TransactionInfoDto()
                .correlationId(null)
                .authorizationRequestId(AUTHORIZATION_REQUEST_ID)
                .paymentMethodName(PaymentMethod.CARDS.serviceName)

        val pspInfo = PspInfoDto().pspId(PSP_ID)

        val transactionResultDto =
            TransactionResultDto().transactionInfo(transactionInfo).pspInfo(pspInfo)

        given(ecommerceTransactionDataProviderV2.findResult(any(), eq(0), eq(1)))
            .willReturn(Mono.just(listOf(transactionResultDto)))

        StepVerifier.create(ecommerceService.searchNpgOperations(TRANSACTION_ID))
            .expectError(NoResultFoundException::class.java)
            .verify()
    }
}
