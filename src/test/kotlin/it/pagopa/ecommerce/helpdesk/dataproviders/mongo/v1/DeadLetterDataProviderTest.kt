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
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @Test
    fun `Should calculate total records for all dead letter events with time range but no exclude statuses and payment gateway`() {
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
                deadLetterRepository
                    .countAllDeadLetterEventInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = emptySet(),
                        npgStatusesToExclude = emptySet(),
                        paymentGatewayToExclude = emptySet()
                    )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countAllDeadLetterEventInTimeRangeWithExcludeStatusesAndPaymentGateway(
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptySet(),
                emptySet(),
                emptySet()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @Test
    fun `Should calculate total records for all dead letter events with time range, exclude statuses and payment gateway`() {
        val count = 3L
        val excludeStatuses =
            DeadLetterExcludedStatusesDto()
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
                .excludedStatuses(excludeStatuses)
                .excludedPaymentGateway(listOf("REDIRECT"))

        given(
                deadLetterRepository
                    .countAllDeadLetterEventInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = setOf("AUTHORIZED", "EXPIRED"),
                        npgStatusesToExclude = setOf("NOTIFIED_KO"),
                        paymentGatewayToExclude = setOf("REDIRECT")
                    )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countAllDeadLetterEventInTimeRangeWithExcludeStatusesAndPaymentGateway(
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                setOf("AUTHORIZED", "EXPIRED"),
                setOf("NOTIFIED_KO"),
                setOf("REDIRECT")
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
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should calculate total records for specific source dead letter events with time range but no exclude statuses and payment gateway`(
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
                deadLetterRepository
                    .countDeadLetterEventForQueueInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        queueName = source,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = emptySet(),
                        npgStatusesToExclude = emptySet(),
                        emptySet()
                    )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countDeadLetterEventForQueueInTimeRangeWithExcludeStatusesAndPaymentGateway(
                source,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptySet(),
                emptySet(),
                emptySet()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should calculate total records for specific source dead letter events with time range, exclude statuses and payment gateway`(
        source: String
    ) {
        val count = 2L
        val excludeStatuses =
            DeadLetterExcludedStatusesDto()
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
                .excludedStatuses(excludeStatuses)
                .excludedPaymentGateway(listOf("REDIRECT"))

        given(
                deadLetterRepository
                    .countDeadLetterEventForQueueInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        queueName = source,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = setOf("UNAUTHORIZED"),
                        npgStatusesToExclude = setOf("TIMEOUT"),
                        paymentGatewayToExclude = setOf("REDIRECT")
                    )
            )
            .willReturn(mono { count })

        StepVerifier.create(deadLetterDataProvider.totalRecordCount(searchRequest))
            .expectNext(count.toInt())
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .countDeadLetterEventForQueueInTimeRangeWithExcludeStatusesAndPaymentGateway(
                source,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                setOf("UNAUTHORIZED"),
                setOf("TIMEOUT"),
                setOf("REDIRECT")
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
            .findDeadLetterEventPaginatedOrderByInsertionDateDesc(skip, limit)
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @Test
    fun `Should find result for all dead letter events with time range but no exclude statuses and payment gateway`() {
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
                    .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = emptySet(),
                        npgStatusesToExclude = emptySet(),
                        paymentGatewayToExclude = emptySet()
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptySet(),
                emptySet(),
                emptySet()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @Test
    fun `Should find result for all dead letter events with time range, exclude statuses and payment gateway`() {
        val excludeStatuses =
            DeadLetterExcludedStatusesDto()
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
                .excludedStatuses(excludeStatuses)
                .excludedPaymentGateway(listOf("REDIRECT"))

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
                    .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = setOf("AUTHORIZED", "EXPIRED"),
                        npgStatusesToExclude = setOf("KO"),
                        setOf("REDIRECT")
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                setOf("AUTHORIZED", "EXPIRED"),
                setOf("KO"),
                setOf("REDIRECT")
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
    fun `Should find result for specific source dead letter events with time range but no exclude statuses and payment gateway`(
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
                    .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        queueName = source,
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = emptySet(),
                        npgStatusesToExclude = emptySet(),
                        paymentGatewayToExclude = emptySet()
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                source,
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                emptySet(),
                emptySet(),
                emptySet()
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ECOMMERCE", "NOTIFICATIONS_SERVICE"])
    fun `Should find result for specific source dead letter events with time range, exclude statuses and payment gateway`(
        source: String
    ) {
        val excludeStatuses =
            DeadLetterExcludedStatusesDto()
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
                .excludedStatuses(excludeStatuses)
                .excludedPaymentGateway(listOf("REDIRECT"))

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
                    .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                        queueName = source,
                        skip = skip,
                        limit = limit,
                        startTime = OffsetDateTime.MIN.toString(),
                        endTime = OffsetDateTime.MAX.toString(),
                        ecommerceStatusesToExclude = setOf("EXPIRED"),
                        npgStatusesToExclude = setOf("TIMEOUT"),
                        paymentGatewayToExclude = setOf("REDIRECT")
                    )
            )
            .willReturn(Flux.fromIterable(deadLetterEvents))

        StepVerifier.create(deadLetterDataProvider.findResult(searchRequest, skip, limit))
            .expectNext(expectedDeadLetterDtoList)
            .verifyComplete()

        verify(deadLetterRepository, times(1))
            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatusesAndPaymentGateway(
                source,
                skip,
                limit,
                OffsetDateTime.MIN.toString(),
                OffsetDateTime.MAX.toString(),
                setOf("EXPIRED"),
                setOf("TIMEOUT"),
                setOf("REDIRECT")
            )
        verifyNoMoreInteractions(deadLetterRepository)
    }
}
