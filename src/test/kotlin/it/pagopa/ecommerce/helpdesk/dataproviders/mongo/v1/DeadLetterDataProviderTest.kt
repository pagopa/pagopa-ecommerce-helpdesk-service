package it.pagopa.ecommerce.helpdesk.dataproviders.mongo.v1

import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.DeadLetterRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.DeadLetterDataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import java.util.EnumMap
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

        verify(deadLetterRepository, times(0))
            .countAllDeadLetterEventInTimeRangeWithExludeStatuses(any(), any(), any(), any())
        verify(deadLetterRepository, times(0)).countDeadLetterEventForQueue(any())
        verify(deadLetterRepository, times(0))
            .countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }

    @Test
    fun `Should calculate total records for all dead letter events with time range but no exclude statuses`() {
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
                deadLetterRepository.countAllDeadLetterEventInTimeRangeWithExludeStatuses(
                    startTime = OffsetDateTime.MIN.toString(),
                    endTime = OffsetDateTime.MAX.toString(),
                    ecommerceStatusesToExclude = emptyList(),
                    npgStatusesToExclude = emptyList()
                )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(0)).count()
        verify(deadLetterRepository, times(1))
            .countAllDeadLetterEventInTimeRangeWithExludeStatuses(
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptyList(),
                emptyList()
            )
        verify(deadLetterRepository, times(0)).countDeadLetterEventForQueue(any())
        verify(deadLetterRepository, times(0))
            .countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }

    @Test
    fun `Should calculate total records for all dead letter events with time range and exclude statuses`() {
        val count = 3L
        val excludeStatuses =
            DeadLetterExcludeStatusesDto()
                .ecommerceStatuses(listOf("AUTHORIZED", "EXPIRED"))
                .npgStatuses(listOf("NOTIFIED_KO"))

        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
                .excludeStatuses(excludeStatuses)

        given(
                deadLetterRepository.countAllDeadLetterEventInTimeRangeWithExludeStatuses(
                    startTime = OffsetDateTime.MIN.toString(),
                    endTime = OffsetDateTime.MAX.toString(),
                    ecommerceStatusesToExclude = listOf("AUTHORIZED", "EXPIRED"),
                    npgStatusesToExclude = listOf("NOTIFIED_KO")
                )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countAllDeadLetterEventInTimeRangeWithExludeStatuses(
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                listOf("AUTHORIZED", "EXPIRED"),
                listOf("NOTIFIED_KO")
            )
        verifyNoMoreInteractions(deadLetterRepository)
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

        verify(deadLetterRepository, times(1)).countDeadLetterEventForQueue(source)
        verify(deadLetterRepository, times(0)).count()
        verify(deadLetterRepository, times(0))
            .countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                any(),
                any(),
                any(),
                any(),
                any()
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should calculate total records for specific source dead letter events with time range but no exclude statuses`(
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
                deadLetterRepository.countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                    queueName = source,
                    startTime = OffsetDateTime.MIN.toString(),
                    endTime = OffsetDateTime.MAX.toString(),
                    ecommerceStatusesToExclude = emptyList(),
                    npgStatusesToExclude = emptyList()
                )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                source,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptyList(),
                emptyList()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should calculate total records for specific source dead letter events with time range and exclude statuses`(
        source: String
    ) {
        val count = 2L
        val excludeStatuses =
            DeadLetterExcludeStatusesDto()
                .ecommerceStatuses(listOf("UNAUTHORIZED"))
                .npgStatuses(listOf("TIMEOUT"))

        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.valueOf(source))
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
                .excludeStatuses(excludeStatuses)

        given(
                deadLetterRepository.countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                    queueName = source,
                    startTime = OffsetDateTime.MIN.toString(),
                    endTime = OffsetDateTime.MAX.toString(),
                    ecommerceStatusesToExclude = listOf("UNAUTHORIZED"),
                    npgStatusesToExclude = listOf("TIMEOUT")
                )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                source,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                listOf("UNAUTHORIZED"),
                listOf("TIMEOUT")
            )
        verifyNoMoreInteractions(deadLetterRepository)
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
            .findDeadLetterEventPaginatedOrderByInsertionDateDesc(skip, limit)
        verify(deadLetterRepository, times(0))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @Test
    fun `Should find result for all dead letter events with time range but no exclude statuses`() {
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
                )
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10

        given(
                deadLetterRepository
                    .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = emptySet(),
                        npgStatusesToExclude = emptySet()
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptySet(),
                emptySet()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @Test
    fun `Should find result for all dead letter events with time range and exclude statuses`() {
        val excludeStatuses =
            DeadLetterExcludeStatusesDto()
                .ecommerceStatuses(listOf("AUTHORIZED", "EXPIRED"))
                .npgStatuses(listOf("KO"))
        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ALL)
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
                .excludeStatuses(excludeStatuses)

        val deadLetterEvents =
            listOf(
                HelpdeskTestUtils.buildDeadLetterEvent(
                    "queue1",
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.NPG
                )
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10

        given(
                deadLetterRepository
                    .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = setOf("AUTHORIZED", "EXPIRED"),
                        npgStatusesToExclude = setOf("KO")
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                setOf("AUTHORIZED", "EXPIRED"),
                setOf("KO")
            )
        verifyNoMoreInteractions(deadLetterRepository)
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
                    source,
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.NPG
                )
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

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(source, skip, limit)
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should find result for specific source dead letter events with time range but no exclude statuses`(
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
                    source,
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.REDIRECT
                )
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10

        given(
                deadLetterRepository
                    .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                        queueName = source,
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = emptySet(),
                        npgStatusesToExclude = emptySet()
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                source,
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptySet(),
                emptySet()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should find result for specific source dead letter events with time range and exclude statuses`(
        source: String
    ) {
        val excludeStatuses =
            DeadLetterExcludeStatusesDto()
                .ecommerceStatuses(listOf("EXPIRED"))
                .npgStatuses(listOf("TIMEOUT"))

        val searchRequest =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.valueOf(source))
                .timeRange(
                    DeadLetterSearchDateTimeRangeDto()
                        .startDate(OffsetDateTime.MIN)
                        .endDate(OffsetDateTime.MAX)
                )
                .excludeStatuses(excludeStatuses)

        val deadLetterEvents =
            listOf(
                HelpdeskTestUtils.buildDeadLetterEvent(
                    source,
                    "test1",
                    TransactionAuthorizationRequestData.PaymentGateway.VPOS,
                    true
                )
            )
        val expectedDeadLetterDtoList =
            deadLetterEvents.map { deadLetterDataProvider.mapToDeadLetterEventDto(it) }
        val skip = 0
        val limit = 10

        given(
                deadLetterRepository
                    .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                        queueName = source,
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = setOf("EXPIRED"),
                        npgStatusesToExclude = setOf("TIMEOUT")
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                source,
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                setOf("EXPIRED"),
                setOf("TIMEOUT")
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }
}
