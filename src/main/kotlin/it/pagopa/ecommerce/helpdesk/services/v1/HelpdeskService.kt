package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.CountInfo
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.mdcutilities.HelpdeskServiceTracingUtils
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
            if (logger.isDebugEnabled) {
                HelpdeskServiceTracingUtils.withContextDetailsMdc(
                    mapOf(
                        "ecommerceCountInfo" to ecommerceCountInfo,
                        "pmCountInfo" to pmCountInfo,
                        "pageNumber" to pageNumber,
                        "pageSize" to pageSize
                    )
                ) {
                    logger.debug(
                        "Requested page number: {}, page size: {}, records to be skipped: {}. Total records found into ecommerce DB: {}, PM DB: {}",
                        pageNumber,
                        pageSize,
                        skip,
                        ecommerceCountInfo,
                        pmCountInfo
                    )
                }
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
                                SearchParamDecoder(
                                    searchParameter = searchTransactionRequestDto,
                                    confidentialMailUtils = confidentialMailUtils
                                ),
                            skip = skip,
                            limit = pageSize,
                            countInfo = ecommerceCountInfo
                        )
                        .doOnSuccess { _ ->
                            HelpdeskServiceTracingUtils.withContextDetailsMdc(
                                mapOf(
                                    "ecommerceCountInfo" to ecommerceCountInfo,
                                    "pageNumber" to pageNumber,
                                    "pageSize" to pageSize,
                                    "skip" to skip
                                ),
                                mapOf(
                                    HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to
                                        "transactionView-mongo-repository",
                                    HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME.key to
                                        "success"
                                )
                            ) {
                                logger.info("Record recovered from eCommerce DB.")
                            }
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
                                HelpdeskServiceTracingUtils.withContextDetailsMdc(
                                    mapOf(
                                        "ecommerceCountInfo" to ecommerceCountInfo,
                                        "pageNumber" to pageNumber,
                                        "pageSize" to pageSize,
                                        "skip" to skip
                                    ),
                                    mapOf(
                                        HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to
                                            "transactionView-mongo-repository",
                                        HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME
                                            .key to "success"
                                    )
                                ) {
                                    logger.info("Last page of records recovered from eCommerce DB.")
                                }
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
                                HelpdeskServiceTracingUtils.withContextDetailsMdc(
                                    mapOf(
                                        "ecommerceCountInfo" to ecommerceCountInfo,
                                        "pmCountInfo" to pmCountInfo,
                                        "pageNumber" to pageNumber,
                                        "pageSize" to pageSize,
                                        "skip" to skip,
                                        "records_from_eCommerce" to ecommerceRemainder,
                                        "PM" to pageSize - ecommerceRemainder,
                                    ),
                                    mapOf(
                                        HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to
                                            "transactionView-mongo-repository",
                                        HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME
                                            .key to "success"
                                    )
                                ) {
                                    logger.info(
                                        "Last page from eCommerce DB recovered and first page from PM (partial page)."
                                    )
                                }
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
                            HelpdeskServiceTracingUtils.withContextDetailsMdc(
                                mapOf(
                                    "skipFromPmDB" to skipFromPmDB,
                                    "pmCountInfo" to pmCountInfo,
                                    "pageSize" to pageSize,
                                ),
                                mapOf(
                                    HelpdeskServiceTracingUtils.TracingEntry.DEPENDENCY.key to "PM",
                                    HelpdeskServiceTracingUtils.TracingEntry.EVENT_OUTCOME.key to
                                        "success"
                                )
                            ) {
                                logger.info("Recovered records from PM DB.")
                            }
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
