package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** Service class that recover records from both eCommerce and PM DB merging results */
@Service
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
        return totalEcommerceCount.zipWith(totalPmCount, ::Pair).flatMap {
            (totalEcommerceCount, totalPmCount) ->
            val skip = pageNumber * pageSize
            logger.info(
                "Requested page number: {}, page size: {}, records to be skipped: {}. Total records found into ecommerce DB: {}, PM DB: {}",
                pageNumber,
                pageSize,
                skip,
                totalEcommerceCount,
                totalPmCount
            )
            val (ecommerceTotalPages, ecommerceRemainder) =
                calculatePages(pageSize = pageSize, totalCount = totalEcommerceCount)
            val records =
                if (pageNumber < ecommerceTotalPages - 1) {
                    logger.info("Recovering records from eCommerce DB. Skip: {}", skip)
                    ecommerceTransactionDataProvider.findResult(
                        searchParams = searchTransactionRequestDto,
                        skip = skip,
                        limit = pageSize
                    )
                } else if (pageNumber == ecommerceTotalPages - 1) {
                    if (ecommerceRemainder == 0) {
                        logger.info(
                            "Recovering last page of records from eCommerce DB, Skip: {}",
                            skip
                        )
                        ecommerceTransactionDataProvider.findResult(
                            searchParams = searchTransactionRequestDto,
                            skip = skip,
                            limit = pageSize
                        )
                    } else {
                        logger.info(
                            "Recovering last page from eCommerce DB and first page from PM (partial page). Records to recover from eCommerce: {}, from PM: {}",
                            ecommerceRemainder,
                            pageSize - ecommerceRemainder
                        )
                        ecommerceTransactionDataProvider
                            .findResult(
                                searchParams = searchTransactionRequestDto,
                                skip = skip,
                                limit = ecommerceRemainder
                            )
                            .flatMap { ecommerceRecords ->
                                pmTransactionDataProvider
                                    .findResult(
                                        searchParams = searchTransactionRequestDto,
                                        skip = 0,
                                        limit = pageSize - ecommerceRemainder
                                    )
                                    .map { pmRecords -> ecommerceRecords + pmRecords }
                            }
                    }
                } else {
                    val skipFromPmDB = skip - totalEcommerceCount
                    logger.info("Recovering records from PM DB, Skip: {}", skipFromPmDB)
                    pmTransactionDataProvider.findResult(
                        searchParams = searchTransactionRequestDto,
                        skip = skipFromPmDB,
                        limit = pageSize
                    )
                }
            return@flatMap records.map { results ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalEcommerceCount + totalPmCount,
                    results = results
                )
            }
        }
    }

    /**
     * Calculate pages for display all records given a page size. Return a Pair<Int,Int> where first
     * argument is page size, second one is total count /page size remainder
     */
    fun calculatePages(pageSize: Int, totalCount: Int): Pair<Int, Int> {
        val remainder = totalCount % pageSize
        val pages = totalCount / pageSize
        return if (remainder == 0) {
            Pair(pages, remainder)
        } else {
            Pair(pages + 1, remainder)
        }
    }
}
