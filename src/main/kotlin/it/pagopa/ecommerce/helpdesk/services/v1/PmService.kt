package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMBulkTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMPaymentMethodsDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("PmServiceV1")
class PmService(
    @Autowired val pmTransactionDataProvider: PMTransactionDataProvider,
    @Autowired val pmPaymentMethodsDataProvider: PMPaymentMethodsDataProvider,
    @Autowired val pmBulkTransactionDataProvider: PMBulkTransactionDataProvider,
    @Value("\${search.pm.transactionIdRangeMax}") private val transactionIdRangeMax: Int
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        pmSearchTransactionRequestDto: PmSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        return pmTransactionDataProvider
            .totalRecordCount(
                SearchParamDecoder(
                    searchParameter = pmSearchTransactionRequestDto,
                    confidentialMailUtils = null
                )
            )
            .flatMap { countInfo ->
                if (countInfo.totalCount() > 0) {
                    pmTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoder(
                                    searchParameter = pmSearchTransactionRequestDto,
                                    confidentialMailUtils = null
                                ),
                            skip = pageSize * pageNumber,
                            limit = pageSize,
                            countInfo = countInfo
                        )
                        .map { results ->
                            buildTransactionSearchResponse(
                                currentPage = pageNumber,
                                totalCount = countInfo.totalCount().toInt(),
                                pageSize = pageSize,
                                results = results
                            )
                        }
                        .doOnSuccess { _ ->
                            LogTracingUtils.withContextDetailsMdc(
                                mapOf(
                                    "total_count" to countInfo.totalCount().toInt(),
                                    "page_number" to pageNumber,
                                    "page_size" to pageSize,
                                    "search_transaction_method" to
                                        pmSearchTransactionRequestDto.type
                                ),
                                mapOf(
                                    LogTracingUtils.TracingEntry.DEPENDENCY.key to
                                        "transactionView-mongo-repository",
                                    LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                                )
                            ) {
                                logger.info("[helpDesk pm service] searchTransaction method,")
                            }
                        }
                } else {
                    Mono.error(NoResultFoundException(pmSearchTransactionRequestDto.type))
                }
            }
    }

    fun searchPaymentMethod(
        pmSearchPaymentMethodRequestDto: PmSearchPaymentMethodRequestDto
    ): Mono<SearchPaymentMethodResponseDto> {
        return pmPaymentMethodsDataProvider
            .findResult(pmSearchPaymentMethodRequestDto)
            .doOnSuccess { _ ->
                LogTracingUtils.withContextDetailsMdc(
                    mapOf(
                        "searchPaymentMethods_search_type" to pmSearchPaymentMethodRequestDto.type,
                    ),
                    mapOf(
                        LogTracingUtils.TracingEntry.DEPENDENCY.key to "PM_database",
                        LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                    )
                ) {
                    logger.info("[helpDesk pm service] searchPaymentMethod done successfully!")
                }
            }
    }

    fun searchBulkTransaction(
        pmSearchBulkTransactionRequestDto: PmSearchBulkTransactionRequestDto
    ): Mono<List<TransactionBulkResultDto>> {

        val isTransactionIdRangeExceeded =
            pmSearchBulkTransactionRequestDto is SearchTransactionRequestTransactionIdRangeDto &&
                (pmSearchBulkTransactionRequestDto.transactionIdRange.endTransactionId.toLong() -
                    pmSearchBulkTransactionRequestDto.transactionIdRange.startTransactionId
                        .toLong()) > transactionIdRangeMax

        return Mono.just(isTransactionIdRangeExceeded)
            .filter { !it }
            .switchIfEmpty(
                Mono.error(NoResultFoundException(pmSearchBulkTransactionRequestDto.type))
            )
            .flatMap {
                pmBulkTransactionDataProvider
                    .findResult(pmSearchBulkTransactionRequestDto)
                    .doOnSuccess { _ ->
                        LogTracingUtils.withContextDetailsMdc(
                            mapOf(
                                "searchBulkTransaction_search_type" to
                                    pmSearchBulkTransactionRequestDto.type,
                            ),
                            mapOf(
                                LogTracingUtils.TracingEntry.DEPENDENCY.key to "PM_database",
                                LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                            )
                        ) {
                            logger.info(
                                "[helpDesk pm service] searchBulkTransaction done successfully!"
                            )
                        }
                    }
            }
            .map { transactionList ->
                transactionList
                    .groupBy { it.id }
                    .map { (id, transactions) ->
                        val baseTransaction = transactions.first()
                        val aggregatedDetails =
                            transactions.flatMap { it.paymentInfo.details.orEmpty() }
                        TransactionBulkResultDto()
                            .id(baseTransaction.id)
                            .userInfo(baseTransaction.userInfo)
                            .transactionInfo(baseTransaction.transactionInfo)
                            .paymentInfo(
                                PaymentInfoDto()
                                    .origin(baseTransaction.paymentInfo.origin)
                                    .details(aggregatedDetails)
                            )
                            .pspInfo(baseTransaction.pspInfo)
                            .product(baseTransaction.product)
                    }
            }
    }
}
