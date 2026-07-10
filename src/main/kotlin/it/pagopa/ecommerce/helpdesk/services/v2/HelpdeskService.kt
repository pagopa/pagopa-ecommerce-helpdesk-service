package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.CountInfo
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.PmTransactionHistoryDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.PageUtils
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** Service class that recover records from both eCommerce and PM DB merging results */
@Service("HelpdeskServiceV2")
class HelpdeskService(
    @Autowired val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired val pmTransactionDataProvider: PMTransactionDataProvider,
    @Autowired
    @Qualifier("confidential-data-manager-client-email")
    private val confidentialDataManagerEmail: ConfidentialDataManager,
    @Autowired
    @Qualifier("confidential-data-manager-client-fiscal-code")
    private val confidentialDataManagerFiscalCode: ConfidentialDataManager,
    @Autowired val pmEcommerceHistoryDataProvider: PmTransactionHistoryDataProvider
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
                .onErrorResume(InvalidSearchCriteriaException::class.java) {
                    Mono.just(CountInfo(0, 0))
                }

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
                .onErrorResume(InvalidSearchCriteriaException::class.java) {
                    Mono.just(CountInfo(0, 0))
                }
        return totalEcommerceCount.zipWith(totalPmCount, ::Pair).flatMap {
            (ecommerceCountInfo, pmCountInfo) ->
            if (pmCountInfo.totalCount() + ecommerceCountInfo.totalCount() == 0L) {
                return@flatMap Mono.error(NoResultFoundException(searchTransactionRequestDto.type))
            }
            val skip = pageNumber * pageSize
            logger.info(
                "Requested page number: {}, page size: {}, records to be skipped: {}. Total records found into ecommerce DB: {}, PM DB: {}",
                pageNumber,
                pageSize,
                skip,
                ecommerceCountInfo,
                pmCountInfo
            )
            val (ecommerceTotalPages, ecommerceRemainder) =
                PageUtils.calculatePages(
                    pageSize = pageSize,
                    totalCount = ecommerceCountInfo.totalCount().toInt()
                )
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
                            limit = pageSize,
                            countInfo = ecommerceCountInfo,
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
                                limit = pageSize,
                                countInfo = ecommerceCountInfo,
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
                                limit = ecommerceRemainder,
                                countInfo = ecommerceCountInfo,
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
                                        limit = pageSize - ecommerceRemainder,
                                        countInfo = pmCountInfo,
                                    )
                                    .map { pmRecords -> ecommerceRecords + pmRecords }
                                    .onErrorResume(InvalidSearchCriteriaException::class.java) {
                                        Mono.just(ecommerceRecords)
                                    }
                            }
                    }
                } else {
                    val skipFromPmDB = skip - ecommerceCountInfo.totalCount()
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
                            skip = skipFromPmDB.toInt(),
                            limit = pageSize,
                            countInfo = pmCountInfo,
                        )
                        .onErrorResume(InvalidSearchCriteriaException::class.java) {
                            Mono.just(emptyList())
                        }
                }
            return@flatMap records.map { results ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount =
                        (ecommerceCountInfo.totalCount() + pmCountInfo.totalCount()).toInt(),
                    pageSize = pageSize,
                    results = results
                )
            }
        }
    }
}
