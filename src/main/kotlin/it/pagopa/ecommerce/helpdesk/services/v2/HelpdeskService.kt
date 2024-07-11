package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** Service class that recover records from both eCommerce and PM DB merging results */
@Service("HelpdeskServiceV2")
class HelpdeskService(
    @Autowired val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired val confidentialDataManager: ConfidentialDataManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        searchTransactionRequestDto: HelpDeskSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        val confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager)
        val totalEcommerceCountMono =
            ecommerceTransactionDataProvider
                .totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = confidentialMailUtils
                    )
                )
                .onErrorResume(InvalidSearchCriteriaException::class.java) { Mono.just(0) }
        return totalEcommerceCountMono.flatMap { totalEcommerceCount ->
            if (totalEcommerceCount == 0) {
                return@flatMap Mono.error(NoResultFoundException(searchTransactionRequestDto.type))
            }
            val skip = pageNumber * pageSize
            logger.info(
                "Requested page number: {}, page size: {}, records to be skipped: {}. Total records found into ecommerce DB: {}",
                pageNumber,
                pageSize,
                skip,
                totalEcommerceCount
            )
            val (ecommerceTotalPages) =
                calculatePages(pageSize = pageSize, totalCount = totalEcommerceCount)
            val records =
                if (pageNumber < ecommerceTotalPages) {
                    logger.info("Recovering records from eCommerce DB. Skip: {}", skip)
                    ecommerceTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoderV2(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = confidentialMailUtils
                                ),
                            skip = skip,
                            limit = pageSize
                        )
                        .onErrorResume(InvalidSearchCriteriaException::class.java) {
                            Mono.just(emptyList())
                        }
                } else {
                    logger.info("Wrong page number, return empty list.")
                    Mono.just(emptyList())
                }
            return@flatMap records.map { results ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalEcommerceCount,
                    pageSize = pageSize,
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
