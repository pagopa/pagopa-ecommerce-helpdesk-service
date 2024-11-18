package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** Service class that recover records from both eCommerce and PM DB merging results */
@Service("HelpdeskServiceV1")
class HelpdeskService(
    @Autowired val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired val pmTransactionDataProvider: PMTransactionDataProvider,
    @Autowired val confidentialDataManager: ConfidentialDataManager,
    @Autowired val npgClient: NpgClient,
    @Autowired val npgApiKeyConfiguration: NpgApiKeyConfiguration
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        searchTransactionRequestDto: HelpDeskSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        val confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager)
        val totalEcommerceCount =
            ecommerceTransactionDataProvider
                .totalRecordCount(
                    SearchParamDecoder(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = confidentialMailUtils
                    )
                )
                .onErrorResume(InvalidSearchCriteriaException::class.java) { Mono.just(0) }
        val totalPmCount =
            pmTransactionDataProvider
                .totalRecordCount(
                    SearchParamDecoder(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = null
                    )
                )
                .onErrorResume(InvalidSearchCriteriaException::class.java) { Mono.just(0) }
        return totalEcommerceCount.zipWith(totalPmCount, ::Pair).flatMap {
            (totalEcommerceCount, totalPmCount) ->
            if (totalPmCount + totalEcommerceCount == 0) {
                return@flatMap Mono.error(NoResultFoundException(searchTransactionRequestDto.type))
            }
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
                    ecommerceTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoder(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = confidentialMailUtils
                                ),
                            skip = skip,
                            limit = pageSize
                        )
                        .onErrorResume(InvalidSearchCriteriaException::class.java) {
                            Mono.just(emptyList())
                        }
                } else if (pageNumber == ecommerceTotalPages - 1) {
                    if (ecommerceRemainder == 0) {
                        logger.info(
                            "Recovering last page of records from eCommerce DB, Skip: {}",
                            skip
                        )
                        ecommerceTransactionDataProvider
                            .findResult(
                                searchParams =
                                    SearchParamDecoder(
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
                        logger.info(
                            "Recovering last page from eCommerce DB and first page from PM (partial page). Records to recover from eCommerce: {}, from PM: {}",
                            ecommerceRemainder,
                            pageSize - ecommerceRemainder
                        )
                        ecommerceTransactionDataProvider
                            .findResult(
                                searchParams =
                                    SearchParamDecoder(
                                        searchParameter = searchTransactionRequestDto,
                                        confidentialMailUtils = confidentialMailUtils
                                    ),
                                skip = skip,
                                limit = ecommerceRemainder
                            )
                            .onErrorResume(InvalidSearchCriteriaException::class.java) {
                                Mono.just(emptyList())
                            }
                            .flatMap { ecommerceRecords ->
                                pmTransactionDataProvider
                                    .findResult(
                                        searchParams =
                                            SearchParamDecoder(
                                                searchParameter = searchTransactionRequestDto,
                                                confidentialMailUtils = null
                                            ),
                                        skip = 0,
                                        limit = pageSize - ecommerceRemainder
                                    )
                                    .map { pmRecords -> ecommerceRecords + pmRecords }
                                    .onErrorResume(InvalidSearchCriteriaException::class.java) {
                                        Mono.just(ecommerceRecords)
                                    }
                            }
                    }
                } else {
                    val skipFromPmDB = skip - totalEcommerceCount
                    logger.info("Recovering records from PM DB, Skip: {}", skipFromPmDB)
                    pmTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoder(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = null
                                ),
                            skip = skipFromPmDB,
                            limit = pageSize
                        )
                        .onErrorResume(InvalidSearchCriteriaException::class.java) {
                            Mono.just(emptyList())
                        }
                }
            return@flatMap records.map { results ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalEcommerceCount + totalPmCount,
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
