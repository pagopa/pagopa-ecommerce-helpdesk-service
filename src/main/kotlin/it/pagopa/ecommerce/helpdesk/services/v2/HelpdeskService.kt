package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.PmTransactionHistoryDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import jakarta.inject.Inject
import jakarta.inject.Named
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** Service class that recover records from both eCommerce and PM DB merging results */
@ApplicationScoped
@Named("HelpdeskServiceV2")
class HelpdeskService(
    @Inject val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Inject val pmTransactionDataProvider: PMTransactionDataProvider,
    @Inject
    @Qualifier("confidential-data-manager-client-email")
    private val confidentialDataManagerEmail: ConfidentialDataManager,
    @Inject
    @Qualifier("confidential-data-manager-client-fiscal-code")
    private val confidentialDataManagerFiscalCode: ConfidentialDataManager,
    @Inject val pmEcommerceHistoryDataProvider: PmTransactionHistoryDataProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        searchTransactionRequestDto: HelpDeskSearchTransactionRequestDto,
        pmProviderType: PmProviderType = PmProviderType.PM_LEGACY
    ): Mono<SearchTransactionResponseDto> {
        val confidentialMailUtils = ConfidentialMailUtils(confidentialDataManagerEmail)
        val confidentialFiscalCodeUtils =
            ConfidentialFiscalCodeUtils(confidentialDataManagerFiscalCode)
        val totalEcommerceCount =
            ecommerceTransactionDataProvider
                .totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = confidentialMailUtils,
                        confidentialFiscalCodeUtils = confidentialFiscalCodeUtils
                    )
                )
                .onErrorResume(InvalidSearchCriteriaException::class.java) { Mono.just(0) }

        val totalPmCount =
            when (pmProviderType) {
                    PmProviderType.PM_LEGACY -> pmTransactionDataProvider
                    PmProviderType.ECOMMERCE_HISTORY -> pmEcommerceHistoryDataProvider
                }
                .totalRecordCount(
                    SearchParamDecoderV2(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = null,
                        confidentialFiscalCodeUtils = null
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
                                SearchParamDecoderV2(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = confidentialMailUtils,
                                    confidentialFiscalCodeUtils = confidentialFiscalCodeUtils
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
                                    SearchParamDecoderV2(
                                        searchParameter = searchTransactionRequestDto,
                                        confidentialMailUtils = confidentialMailUtils,
                                        confidentialFiscalCodeUtils = confidentialFiscalCodeUtils
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
                                    SearchParamDecoderV2(
                                        searchParameter = searchTransactionRequestDto,
                                        confidentialMailUtils = confidentialMailUtils,
                                        confidentialFiscalCodeUtils = confidentialFiscalCodeUtils
                                    ),
                                skip = skip,
                                limit = ecommerceRemainder
                            )
                            .onErrorResume(InvalidSearchCriteriaException::class.java) {
                                Mono.just(emptyList())
                            }
                            .flatMap { ecommerceRecords ->
                                when (pmProviderType) {
                                        PmProviderType.PM_LEGACY -> pmTransactionDataProvider
                                        PmProviderType.ECOMMERCE_HISTORY ->
                                            pmEcommerceHistoryDataProvider
                                    }
                                    .findResult(
                                        searchParams =
                                            SearchParamDecoderV2(
                                                searchParameter = searchTransactionRequestDto,
                                                confidentialMailUtils = null,
                                                confidentialFiscalCodeUtils = null
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
                    when (pmProviderType) {
                            PmProviderType.PM_LEGACY -> pmTransactionDataProvider
                            PmProviderType.ECOMMERCE_HISTORY -> pmEcommerceHistoryDataProvider
                        }
                        .findResult(
                            searchParams =
                                SearchParamDecoderV2(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = null,
                                    confidentialFiscalCodeUtils = null
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
