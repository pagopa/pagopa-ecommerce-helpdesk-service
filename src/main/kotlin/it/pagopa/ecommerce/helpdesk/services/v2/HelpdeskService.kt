package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
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

            if (logger.isDebugEnabled) {
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(
                        mapOf(
                            "page_number" to pageNumber.toString(),
                            "page_size" to pageSize.toString(),
                            "skip" to skip.toString(),
                            "ecommerce_count_info" to ecommerceCountInfo.toString(),
                            "pm_count_info" to pmCountInfo.toString()
                        )
                    )
                    .logDebug(logger, "Requested page number details")
            }

            val (ecommerceTotalPages, ecommerceRemainder) =
                PageUtils.calculatePages(
                    pageSize = pageSize,
                    totalCount = ecommerceCountInfo.totalCount().toInt()
                )
            val records =
                if (pageNumber < ecommerceTotalPages - 1) {
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
                        .doOnSuccess { _ ->
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf(
                                        "skip" to skip.toString(),
                                        "limit" to pageSize.toString(),
                                        "count_info" to ecommerceCountInfo.toString()
                                    )
                                )
                                .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                                .logInfo(logger, "Records recovered from eCommerce DB successfully")
                        }
                        .onErrorResume(InvalidSearchCriteriaException::class.java) {
                            Mono.just(emptyList())
                        }
                } else if (pageNumber == ecommerceTotalPages - 1) {
                    if (ecommerceRemainder == 0) {
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
                            .doOnSuccess { _ ->
                                LogTracingUtils.loggerTracingUtils()
                                    .success()
                                    .details(
                                        mapOf(
                                            "skip" to skip.toString(),
                                            "limit" to pageSize.toString(),
                                            "count_info" to ecommerceCountInfo.toString()
                                        )
                                    )
                                    .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                                    .logInfo(
                                        logger,
                                        "Last page of records recovered from eCommerce DB"
                                    )
                            }
                            .onErrorResume(InvalidSearchCriteriaException::class.java) {
                                Mono.just(emptyList())
                            }
                    } else {
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
                            .doOnSuccess { _ ->
                                LogTracingUtils.loggerTracingUtils()
                                    .success()
                                    .details(
                                        mapOf(
                                            "ecommerce_records" to ecommerceRemainder.toString(),
                                            "pm_records" to
                                                (pageSize - ecommerceRemainder).toString(),
                                        )
                                    )
                                    .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                                    .logInfo(
                                        logger,
                                        "Last page from eCommerce DB and first page from PM (partial page) recovered"
                                    )
                            }
                    }
                } else {
                    val skipFromPmDB = skip - ecommerceCountInfo.totalCount()
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
                        .doOnSuccess { _ ->
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf(
                                        "skip" to skipFromPmDB.toString(),
                                        "limit" to pageSize.toString(),
                                        "count_info" to pmCountInfo.toString()
                                    )
                                )
                                .dependency("PM-db")
                                .logInfo(logger, "Records recovered from PM DB successfully")
                        }
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
