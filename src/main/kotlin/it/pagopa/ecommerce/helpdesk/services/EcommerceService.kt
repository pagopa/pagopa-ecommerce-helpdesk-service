package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.buildDeadLetterEventsSearchResponse
import it.pagopa.ecommerce.helpdesk.utils.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.*
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EcommerceService(
    @Autowired private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired private val deadLetterDataProvider: DeadLetterDataProvider,
    @Autowired private val confidentialDataManager: ConfidentialDataManager
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
                    SearchParamDecoder(
                        searchParameter = ecommerceSearchTransactionRequestDto,
                        confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager)
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

    fun searchDeadLetterEvents(
        pageNumber: Int,
        pageSize: Int,
        searchRequest: EcommerceSearchDeadLetterEventsRequestDto
    ): Mono<SearchDeadLetterEventResponseDto> {
        logger.info(
            "[helpDesk ecommerce service] search dead letter events, type: {}",
            searchRequest.source
        )
        val timeRange: DeadLetterSearchDateTimeRangeDto? = searchRequest.timeRange
        return mono { searchRequest }
            .filter { timeRange == null || timeRange.startDate < timeRange.endDate }
            .switchIfEmpty(
                Mono.error(
                    InvalidSearchCriteriaException(
                        "Invalid time range: startDate [${timeRange?.startDate}] is not greater than endDate: [${timeRange?.endDate}]"
                    )
                )
            )
            .flatMap {
                searchPaginatedResult(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchCriteria = searchRequest,
                    searchCriteriaType = it.toString(),
                    dataProvider = deadLetterDataProvider
                )
            }
            .map { (results, totalCount) ->
                buildDeadLetterEventsSearchResponse(
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
}
