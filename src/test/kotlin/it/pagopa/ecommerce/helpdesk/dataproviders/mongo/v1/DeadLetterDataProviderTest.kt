package it.pagopa.ecommerce.helpdesk.dataproviders.mongo.v1

import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.DeadLetterRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.DeadLetterDataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchDateTimeRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchDeadLetterEventsRequestDto
import java.time.OffsetDateTime
import java.util.*
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class DeadLetterDataProviderTest {

    private val deadLetterRepository: DeadLetterRepository = mock()
    private val deadLetterQueueMapping: EnumMap<DeadLetterSearchEventSourceDto, String> =
        EnumMap(DeadLetterSearchEventSourceDto.values().associateWith { it.toString() })
    private val deadLetterDataProvider =
        DeadLetterDataProvider(
            deadLetterRepository = deadLetterRepository,
            deadLetterQueueMapping = deadLetterQueueMapping
        )

    @Test
    fun `Should calculate total records for all dead letter events without time range`() {
        val count = 2L
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(null)
        given(deadLetterRepository.count()).willReturn(mono { count })
        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()
        verify(deadLetterRepository, times(1)).count()
        verify(deadLetterRepository, times(0)).countAllDeadLetterEventInTimeRange(any(), any())
        verify(deadLetterRepository, times(0)).countDeadLetterEventForQueue(any())
        verify(deadLetterRepository, times(0))
            .countDeadLetterEventForQueueInTimeRange(any(), any(), any())
    }

    @Test
    fun `Should calculate total records for all dead letter events with time range`() {
        val count = 2L
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
        given(
                deadLetterRepository.countAllDeadLetterEventInTimeRange(
                    startTime = OffsetDateTime.MIN.toString(),
                    endTime = OffsetDateTime.MAX.toString(),
                )
            )
            .willReturn(mono { count })
        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()
        verify(deadLetterRepository, times(0)).count()
        verify(deadLetterRepository, times(1)).countAllDeadLetterEventInTimeRange(any(), any())
        verify(deadLetterRepository, times(0)).countDeadLetterEventForQueue(any())
        verify(deadLetterRepository, times(0))
            .countDeadLetterEventForQueueInTimeRange(any(), any(), any())
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should calculate total records for specific source dead letter events without time range`(
        source: String
    ) {
        val count = 2L
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.valueOf(source))
                .timeRange(null)
        given(deadLetterRepository.countDeadLetterEventForQueue(queueName = source))
            .willReturn(mono { count })
        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()
        verify(deadLetterRepository, times(0)).count()
        verify(deadLetterRepository, times(0)).countAllDeadLetterEventInTimeRange(any(), any())
        verify(deadLetterRepository, times(1)).countDeadLetterEventForQueue(any())
        verify(deadLetterRepository, times(0))
            .countDeadLetterEventForQueueInTimeRange(any(), any(), any())
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should calculate total records for specific source dead letter events with time range`(
        source: String
    ) {
        val count = 2L
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.valueOf(source))
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
        given(
                deadLetterRepository.countDeadLetterEventForQueueInTimeRange(
                    queueName = source,
                    startTime = OffsetDateTime.MIN.toString(),
                    endTime = OffsetDateTime.MAX.toString()
                )
            )
            .willReturn(mono { count })
        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()
        verify(deadLetterRepository, times(0)).count()
        verify(deadLetterRepository, times(0)).countAllDeadLetterEventInTimeRange(any(), any())
        verify(deadLetterRepository, times(0)).countDeadLetterEventForQueue(any())
        verify(deadLetterRepository, times(1))
            .countDeadLetterEventForQueueInTimeRange(any(), any(), any())
    }

    @Test
    fun `Should find result for all dead letter events without time range`() {
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(null)
        val deadLetterEvents =
            listOf(
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue1",
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.NPG
                ),
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue2",
                    "test2",
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT,
                    true
                ),
                HelpdeskTestUtils.buildDeadLetterEventWithoutTransactionInfo("queue3", "test3"),
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue4",
                    "test4",
                    TransactionAuthorizationRequestData.PaymentGateway.VPOS
                )
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10
        given(
                deadLetterRepository.findDeadLetterEventPaginatedOrderByInsertionDateDesc(
                    skip = skip,
                    limit = limit
                )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))
        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()
        verify(deadLetterRepository, times(1))
            .findDeadLetterEventPaginatedOrderByInsertionDateDesc(any(), any())
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any()
            )
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(any(), any(), any())
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }

    @Test
    fun `Should find result for all dead letter events with time range`() {
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
        val deadLetterEvents =
            listOf(
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue1",
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.NPG,
                    true
                ),
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue2",
                    "test2",
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT
                ),
                HelpdeskTestUtils.buildDeadLetterEventWithoutTransactionInfo("queue3", "test3")
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10
        given(
                deadLetterRepository
                    .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString()
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))
        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDesc(any(), any())
        verify(deadLetterRepository, times(1))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any()
            )
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(any(), any(), any())
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should find result for specific source dead letter events without time range`(
        source: String
    ) {
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.valueOf(source))
                .timeRange(null)
        val deadLetterEvents =
            listOf(
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue1",
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.NPG
                ),
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue2",
                    "test2",
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT,
                    true
                ),
                HelpdeskTestUtils.buildDeadLetterEventWithoutTransactionInfo("queue3", "test3")
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10
        given(
                deadLetterRepository.findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(
                    queueName = source,
                    skip = skip,
                    limit = limit
                )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))
        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDesc(any(), any())
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any()
            )
        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(any(), any(), any())
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should find result for specific source dead letter events with time range`(
        source: String
    ) {
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.valueOf(source))
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
        val deadLetterEvents =
            listOf(
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue1",
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.NPG,
                    true
                ),
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue2",
                    "test2",
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT
                ),
                HelpdeskTestUtils.buildDeadLetterEventWithoutTransactionInfo("queue3", "test3")
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10
        given(
                deadLetterRepository
                    .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
                        queueName = source,
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString()
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))
        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDesc(any(), any())
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any()
            )
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(any(), any(), any())
        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }
}
