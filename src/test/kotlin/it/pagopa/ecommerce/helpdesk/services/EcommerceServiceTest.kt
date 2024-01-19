package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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

    private val ecommerceService =
        EcommerceService(
            ecommerceTransactionDataProvider,
            deadLetterDataProvider,
            confidentialDataManager
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
                deadLetterDataProvider = deadLetterDataProvider
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
}
