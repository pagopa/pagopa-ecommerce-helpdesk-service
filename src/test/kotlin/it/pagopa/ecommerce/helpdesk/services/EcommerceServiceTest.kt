package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceServiceTest {

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()

    private val deadLetterDataProvider: DeadLetterDataProvider = mock()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val ecommerceService =
        EcommerceService(
            ecommerceTransactionDataProvider,
            deadLetterDataProvider,
            confidentialDataManager
        )

    @Test
    fun `should return found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        val searchParamDecoder =
            SearchParamDecoder(
                searchParameter = searchCriteria,
                confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager)
            )
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactions =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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
}
