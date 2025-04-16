package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchMetricsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionMetricsResponseDto
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("EcommerceServiceV2")
class EcommerceService(
    @Autowired private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired
    @Qualifier("confidential-data-manager-client-email")
    private val confidentialDataManagerEmail: ConfidentialDataManager,
    @Autowired
    @Qualifier("confidential-data-manager-client-fiscal-code")
    private val confidentialDataManagerFiscalCode: ConfidentialDataManager,
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
    ): Mono<Pair<List<V>, Int>> {
        return dataProvider.totalRecordCount(searchCriteria).flatMap { totalCount ->
            if (totalCount > 0) {
                val skip = pageSize * pageNumber
                logger.info(
                    "Total record found: {}, skip: {}, limit: {}",
                    totalCount,
                    skip,
                    pageSize
                )
                dataProvider
                    .findResult(searchParams = searchCriteria, skip = skip, limit = pageSize)
                    .zipWith(mono { totalCount }, ::Pair)
            } else {
                Mono.error(NoResultFoundException(searchCriteriaType))
            }
        }
    }

    fun searchMetrics(
        searchMetricsRequestDto: SearchMetricsRequestDto
    ): Mono<TransactionMetricsResponseDto> {
        logger.info("[helpDesk ecommerce service] searchMetrics method")
        return ecommerceTransactionDataProvider.s
            .map { (results, totalCount) ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalCount,
                    pageSize = pageSize,
                    results = results
                )
            }
    }
}
