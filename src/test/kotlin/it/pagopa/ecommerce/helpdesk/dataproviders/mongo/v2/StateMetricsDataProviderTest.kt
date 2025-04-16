package it.pagopa.ecommerce.helpdesk.dataproviders.mongo.v2

import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.StateMetricsDataProvider
import it.pagopa.ecommerce.helpdesk.documents.EcommerceStatusCount
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchMetricsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchMetricsRequestTimeRangeDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionMetricsResponseDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class StateMetricsDataProviderTest {

    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private lateinit var stateMetricsDataProvider: StateMetricsDataProvider

    @BeforeEach
    fun setup() {
        stateMetricsDataProvider = StateMetricsDataProvider(transactionsViewRepository)
    }

    @Test
    fun `should compute metrics correctly given valid clientId`() {

        val clientId = "CHECKOUT"
        val startDate = OffsetDateTime.now(ZoneOffset.UTC)
        val endDate = OffsetDateTime.now(ZoneOffset.UTC)
        val criteria =
            SearchMetricsRequestDto()
                .clientId(clientId)
                .timeRange(SearchMetricsRequestTimeRangeDto().startDate(startDate).endDate(endDate))

        val metrics =
            listOf(
                EcommerceStatusCount("ACTIVATED", 1),
                EcommerceStatusCount("AUTHORIZATION_REQUESTED", 2),
                EcommerceStatusCount("AUTHORIZATION_COMPLETED", 3),
                EcommerceStatusCount("CLOSURE_REQUESTED", 4),
                EcommerceStatusCount("CLOSED", 5),
                EcommerceStatusCount("CLOSURE_ERROR", 6),
                EcommerceStatusCount("NOTIFIED_OK", 7),
                EcommerceStatusCount("NOTIFIED_KO", 8),
                EcommerceStatusCount("NOTIFICATION_ERROR", 9),
                EcommerceStatusCount("NOTIFICATION_REQUESTED", 10),
                EcommerceStatusCount("EXPIRED", 11),
                EcommerceStatusCount("REFUNDED", 12),
                EcommerceStatusCount("CANCELED", 13),
                EcommerceStatusCount("EXPIRED_NOT_AUTHORIZED", 14),
                EcommerceStatusCount("UNAUTHORIZED", 15),
                EcommerceStatusCount("REFUND_ERROR", 16),
                EcommerceStatusCount("REFUND_REQUESTED", 17),
                EcommerceStatusCount("CANCELLATION_REQUESTED", 18),
                EcommerceStatusCount("CANCELLATION_EXPIRED", 19)
            )

        val expected =
            TransactionMetricsResponseDto()
                .ACTIVATED(1)
                .AUTHORIZATION_REQUESTED(2)
                .AUTHORIZATION_COMPLETED(3)
                .CLOSURE_REQUESTED(4)
                .CLOSED(5)
                .CLOSURE_ERROR(6)
                .NOTIFIED_OK(7)
                .NOTIFIED_KO(8)
                .NOTIFICATION_ERROR(9)
                .NOTIFICATION_REQUESTED(10)
                .EXPIRED(11)
                .REFUNDED(12)
                .CANCELED(13)
                .EXPIRED_NOT_AUTHORIZED(14)
                .UNAUTHORIZED(15)
                .REFUND_ERROR(16)
                .REFUND_REQUESTED(17)
                .CANCELLATION_REQUESTED(18)
                .CANCELLATION_EXPIRED(19)

        whenever(
                transactionsViewRepository.findMetricsGivenStartDateAndEndDateAndClientId(
                    startDate.toString(),
                    endDate.toString(),
                    clientId
                )
            )
            .thenReturn(Flux.fromIterable(metrics))

        val resultMono = stateMetricsDataProvider.computeMetrics(criteria)

        StepVerifier.create(resultMono)
            .expectNextMatches { actual -> actual == expected }
            .verifyComplete()

        verify(transactionsViewRepository)
            .findMetricsGivenStartDateAndEndDateAndClientId(
                startDate.toString(),
                endDate.toString(),
                clientId
            )
    }
}
