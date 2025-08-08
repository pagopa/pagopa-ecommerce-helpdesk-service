package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.StateMetricsDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchMetricsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionMetricsResponseDto
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import jakarta.inject.Inject
import jakarta.inject.Named
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono


@ApplicationScoped
@Named("EcommerceServiceV2")
class EcommerceService(
    @Inject private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Inject
    @Qualifier("confidential-data-manager-client-email")
    private val confidentialDataManagerEmail: ConfidentialDataManager,
    @Inject
    @Qualifier("confidential-data-manager-client-fiscal-code")
    private val confidentialDataManagerFiscalCode: ConfidentialDataManager,
    @Inject private val stateMetricsDataProvider: StateMetricsDataProvider,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return searchPaginatedResult(
                pageNumber = pageNumber,
                pageSize = pageSize,
                searchCriteria =
                    SearchParamDecoderV2(
                        searchParameter = ecommerceSearchTransactionRequestDto,
                        confidentialMailUtils = ConfidentialMailUtils(confidentialDataManagerEmail),
                        confidentialFiscalCodeUtils =
                            ConfidentialFiscalCodeUtils(confidentialDataManagerFiscalCode)
                    ),
                searchCriteriaType = ecommerceSearchTransactionRequestDto.type,
                dataProvider = ecommerceTransactionDataProvider
            )
            .map { (results, totalCount) ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalCount,
                    pageSize = pageSize,
                    results = results
                )
            }
    }

    private fun <K, V> searchPaginatedResult(
        pageNumber: Int,
        pageSize: Int,
        searchCriteria: K,
        dataProvider: DataProvider<K, V>,
        searchCriteriaType: String
    ):  Pair<List<V>, Int> {
            val totalCount = dataProvider.totalRecordCount(searchCriteria)
            if (totalCount > 0) {
                val skip = pageSize * pageNumber
                logger.info("Total record found: {}, skip: {}, limit: {}", totalCount, skip, pageSize)
                val results = dataProvider.findResult(searchCriteria, skip, pageSize)
                return Pair(results, totalCount)
            } else {
                throw NoResultFoundException(searchCriteriaType)
            }
        }
    }

    fun searchMetrics(
        searchMetricsRequestDto: SearchMetricsRequestDto
    ): Mono<TransactionMetricsResponseDto> {
        logger.info("[helpDesk ecommerce service] searchMetrics method")
        return stateMetricsDataProvider.computeMetrics(searchMetricsRequestDto)
    }
}
