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
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf(
                                        "total_count" to countInfo.totalCount().toString(),
                                        "page_number" to pageNumber.toString(),
                                        "page_size" to pageSize.toString(),
                                        "search_transaction_method" to
                                            pmSearchTransactionRequestDto.type
                                    )
                                )
                                .logInfo(
                                    logger,
                                    "[helpDesk pm service] searchTransaction method done successfully!"
                                )
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
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(mapOf("search_type" to pmSearchPaymentMethodRequestDto.type))
                    .dependency("PM-db")
                    .logInfo(logger, "[helpDesk pm service] searchPaymentMethod done successfully!")
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
                        LogTracingUtils.loggerTracingUtils()
                            .success()
                            .details(
                                mapOf(
                                    "search_criteria" to pmSearchBulkTransactionRequestDto.type,
                                )
                            )
                            .dependency("PM-db")
                            .logInfo(
                                logger,
                                "[helpDesk pm service] searchBulkTransaction done successfully!"
                            )
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
