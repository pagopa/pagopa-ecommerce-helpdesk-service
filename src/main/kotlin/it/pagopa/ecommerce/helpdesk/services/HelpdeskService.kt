package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono

class HelpdeskService(
    @Autowired val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired val pmTransactionDataProvider: PMTransactionDataProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        searchTransactionRequestDto: HelpDeskSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        val totalEcommerceCount =
            ecommerceTransactionDataProvider
                .totalRecordCount(searchTransactionRequestDto)
                .onErrorResume(InvalidSearchCriteriaException::class.java) { Mono.just(0) }
        val totalPmCount =
            pmTransactionDataProvider.totalRecordCount(searchTransactionRequestDto).onErrorResume(
                InvalidSearchCriteriaException::class.java
            ) {
                Mono.just(0)
            }
        totalEcommerceCount.zipWith(totalPmCount, ::Pair).flatMap {
            (totalEcommerceCount, totalPmCount) ->
            val ecommerceTotalPages = totalPages(totalEcommerceCount, pageSize)
            val pmTotalPages = totalPages(totalEcommerceCount, pageSize)
            val recordToFetchFromEcommerceDb = 0
            val recordToFetchFromPmDb = 0
            // TODO continua da qui
            return@flatMap Mono.just(0)
        }

        return Mono.empty()
    }

    /**
     * Calculate total pages given total record count and page size Return a Pair<totalPages,isOdd>
     * where is odd flag indicates if last page is odd or not
     */
    private fun totalPages(totalCount: Int, pageSize: Int): Pair<Int, Boolean> =
        if (totalCount % pageSize == 0) {
            Pair(totalCount / pageSize, false)
        } else {
            Pair(totalCount / pageSize + 1, true)
        }
}
