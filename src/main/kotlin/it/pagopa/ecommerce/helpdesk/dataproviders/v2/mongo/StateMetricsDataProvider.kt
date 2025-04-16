package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.helpdesk.dataproviders.MetricsProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.utils.v2.buildSearchMetricsResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component("StateMetricsDataProvider")
class StateMetricsDataProvider(
    @Autowired private val transactionsViewRepository: TransactionsViewRepository,
) : MetricsProvider<SearchMetricsRequestDto, TransactionMetricsResponseDto> {

    override fun computeMetrics(
        criteria: SearchMetricsRequestDto
    ): Mono<TransactionMetricsResponseDto> {

        return transactionsViewRepository
            .findMetricsGivenClientId(criteria.clientId)
            .collectList()
            .map { buildSearchMetricsResponse(it) }
    }
}
