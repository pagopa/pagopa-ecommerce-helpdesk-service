package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.StateMetricsDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.mdcutilities.HelpdeskServiceTracingUtils
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils
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
    @Autowired private val stateMetricsDataProvider: StateMetricsDataProvider,
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
            .doOnSuccess { _ ->
                HelpdeskServiceTracingUtils.withContextDetailsMdc(
                    mapOf(
                        "pageNumber" to pageNumber,
                        "pageSize" to pageSize,
                    ),
                    mapOf(
                        HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to
                            "eCommerce-Mongo-transaction-view-repository",
                        HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                    )
                ) {
                    logger.info(
                        "[helpDesk ecommerce service] searchTransaction method done successfully!"
                    )
                }
            }
    }

    private fun <K, V> searchPaginatedResult(
        pageNumber: Int,
        pageSize: Int,
        searchCriteria: K,
        dataProvider: DataProvider<K, V>,
        searchCriteriaType: String
    ): Mono<Pair<List<V>, Int>> {
        return dataProvider.totalRecordCount(searchCriteria).flatMap { countInfo ->
            if (countInfo.totalCount() > 0) {
                val skip = pageSize * pageNumber
                dataProvider
                    .findResult(
                        searchParams = searchCriteria,
                        skip = skip,
                        limit = pageSize,
                        countInfo = countInfo
                    )
                    .zipWith(mono { countInfo.totalCount().toInt() }, ::Pair)
                    .doOnSuccess { _ ->
                        HelpdeskServiceTracingUtils.withContextDetailsMdc(
                            mapOf(
                                "pageNumber" to pageNumber,
                                "pageSize" to pageSize,
                                "countInfo" to countInfo,
                                "skip" to skip,
                                "searchCriteriaType" to searchCriteriaType
                            ),
                            mapOf(
                                HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to
                                    "eCommerce-Mongo-transaction-view-repository",
                                HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME.key to
                                    "success"
                            )
                        ) {
                            logger.info("searchPaginatedResult done successfully!")
                        }
                    }
            } else {
                Mono.error(NoResultFoundException(searchCriteriaType))
            }
        }
    }

    fun searchMetrics(
        searchMetricsRequestDto: SearchMetricsRequestDto
    ): Mono<TransactionMetricsResponseDto> {
        logger.info("[helpDesk ecommerce service] searchMetrics method")
        return stateMetricsDataProvider.computeMetrics(searchMetricsRequestDto).doOnSuccess { _ ->
            HelpdeskServiceTracingUtils.withContextDetailsMdc(
                null,
                mapOf(
                    HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to
                        "eCommerce-Mongo-transaction-view-repository",
                    HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                )
            ) {
                logger.info("ecommerceSearchTransaction done successfully!")
            }
        }
    }
}
