package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.helpdesk.dataproviders.MetricsProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.utils.v2.buildSearchMetricsResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import jakarta.inject.Inject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import reactor.core.publisher.Mono

@ApplicationScoped
@Named("StateMetricsDataProvider")
class StateMetricsDataProvider(
    @Inject private val transactionsViewRepository: TransactionsViewRepository,
) : MetricsProvider<SearchMetricsRequestDto, TransactionMetricsResponseDto> {

    override fun computeMetrics(
        criteria: SearchMetricsRequestDto
    ): Mono<TransactionMetricsResponseDto> {

        return transactionsViewRepository
            .findMetricsGivenStartDateAndEndDateAndClientIdAndPspIdAndPaymentTypeCode(
                criteria.timeRange.startDate.toString(),
                criteria.timeRange.endDate.toString(),
                criteria.clientId,
                criteria.pspId,
                criteria.paymentTypeCode
            )
            .collectList()
            .map { buildSearchMetricsResponse(it) }
    }
}
