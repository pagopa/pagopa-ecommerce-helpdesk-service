package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.CountInfo
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.LogTracingTags
import it.pagopa.ecommerce.helpdesk.utils.PageUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** Service class that recover records from both eCommerce and PM DB merging results */
@Service("HelpdeskServiceV1")
class HelpdeskService(
    @Autowired val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired val pmTransactionDataProvider: PMTransactionDataProvider,
    @Autowired
    @Qualifier("confidential-data-manager-client-email")
    val confidentialDataManager: ConfidentialDataManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        searchTransactionRequestDto: HelpDeskSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        val confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager)
        val ecommerceCountInfo =
            ecommerceTransactionDataProvider
                .totalRecordCount(
                    SearchParamDecoder(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = confidentialMailUtils
                    )
                )
                .onErrorResume(InvalidSearchCriteriaException::class.java) {
                    Mono.just(CountInfo(0, 0))
                }
        val pmCountInfo =
            pmTransactionDataProvider
                .totalRecordCount(
                    SearchParamDecoder(
                        searchParameter = searchTransactionRequestDto,
                        confidentialMailUtils = null
                    )
                )
                .onErrorResume(InvalidSearchCriteriaException::class.java) {
                    Mono.just(CountInfo(0, 0))
                }
        return ecommerceCountInfo.zipWith(pmCountInfo, ::Pair).flatMap {
            (ecommerceCountInfo, pmCountInfo) ->
            if (pmCountInfo.totalCount() + ecommerceCountInfo.totalCount() == 0L) {
                return@flatMap Mono.error(NoResultFoundException(searchTransactionRequestDto.type))
            }
            val skip = pageNumber * pageSize
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
                                SearchParamDecoder(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = confidentialMailUtils
                                ),
                            skip = skip,
                            limit = pageSize,
                            countInfo = ecommerceCountInfo
                        )
                        .doOnSuccess { _ ->
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf(
                                        "ecommerce_count_info" to ecommerceCountInfo.toString(),
                                        "page_number" to pageNumber.toString(),
                                        "page_size" to pageSize.toString(),
                                        "skip" to skip.toString()
                                    )
                                )
                                .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                                .logInfo(logger, "Record recovered from eCommerce DB")
                        }
                        .onErrorResume(InvalidSearchCriteriaException::class.java) {
                            Mono.just(emptyList())
                        }
                } else if (pageNumber == ecommerceTotalPages - 1) {
                    if (ecommerceRemainder == 0) {
                        ecommerceTransactionDataProvider
                            .findResult(
                                searchParams =
                                    SearchParamDecoder(
                                        searchParameter = searchTransactionRequestDto,
                                        confidentialMailUtils = confidentialMailUtils
                                    ),
                                skip = skip,
                                limit = pageSize,
                                countInfo = ecommerceCountInfo
                            )
                            .doOnSuccess { _ ->
                                LogTracingUtils.loggerTracingUtils()
                                    .success()
                                    .details(
                                        mapOf(
                                            "ecommerce_count_info" to ecommerceCountInfo.toString(),
                                            "page_number" to pageNumber.toString(),
                                            "page_size" to pageSize.toString(),
                                            "skip" to skip.toString()
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
                                    SearchParamDecoder(
                                        searchParameter = searchTransactionRequestDto,
                                        confidentialMailUtils = confidentialMailUtils
                                    ),
                                skip = skip,
                                limit = ecommerceRemainder,
                                countInfo = ecommerceCountInfo
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
                                        limit = pageSize - ecommerceRemainder,
                                        countInfo = pmCountInfo
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
                                            "ecommerce_count_info" to ecommerceCountInfo.toString(),
                                            "pm_count_info" to pmCountInfo.toString(),
                                            "page_number" to pageNumber.toString(),
                                            "page_size" to pageSize.toString(),
                                            "skip" to skip.toString(),
                                            "records_from_eCommerce" to
                                                ecommerceRemainder.toString(),
                                            "records_from_PM" to
                                                (pageSize - ecommerceRemainder).toString()
                                        )
                                    )
                                    .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                                    .logInfo(
                                        logger,
                                        "Last page from eCommerce DB recovered and first page from PM (partial page)"
                                    )
                            }
                    }
                } else {
                    val skipFromPmDB = skip - ecommerceCountInfo.totalCount().toInt()
                    pmTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoder(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = null
                                ),
                            skip = skipFromPmDB,
                            limit = pageSize,
                            countInfo = pmCountInfo
                        )
                        .doOnSuccess { _ ->
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf(
                                        "pm_count_info" to pmCountInfo.toString(),
                                        "page_number" to pageNumber.toString(),
                                        "page_size" to pageSize.toString(),
                                        "skip_from_PM_db" to skipFromPmDB.toString()
                                    )
                                )
                                .dependency(LogTracingTags.Dependency.PM_DB)
                                .logInfo(logger, "Records recovered from PM DB")
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
