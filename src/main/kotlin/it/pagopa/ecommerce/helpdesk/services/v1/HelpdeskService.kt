package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.client.NpgClient.PaymentMethod
import it.pagopa.ecommerce.commons.documents.v2.activation.NpgTransactionGatewayActivationData
import it.pagopa.ecommerce.commons.domain.TransactionId
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransaction
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithPaymentToken
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRequestedAuthorization
import it.pagopa.ecommerce.commons.exceptions.NpgResponseException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.*
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.util.UUID
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
    @Autowired val npgApiKeyConfiguration: NpgApiKeyConfiguration,
    @Autowired val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>,
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
     * Used by the API that searches for NPG operations. Starting from the transaction id, first it
     * retrieves the transaction details calling the [retrieveTransactionDetails] that returns a
     * [NTuple4], then it uses the details to call the [performGetOrderNPG]. Returns a
     * [SearchNpgOperationsResponseDto], representing a subset of [OrderResponseDto] data.
     */
    fun searchNpgOperations(transactionId: String): Mono<SearchNpgOperationsResponseDto> {
        return retrieveTransactionDetails(transactionId)
            .flatMap { details ->
                performGetOrderNPG(
                    transactionId = TransactionId(transactionId),
                    orderId = details.first,
                    pspId = details.second,
                    correlationId = details.third,
                    paymentMethod = details.fourth
                )
            }
            .doOnNext { order ->
                logger.info(
                    "Performed get order for transaction with id: [{}], last operation result: [{}], operations: [{}]",
                    transactionId,
                    order.orderStatus?.lastOperationType,
                    order.operations?.joinToString { "${it.operationType}-${it.operationResult}" },
                )
            }
            .map { orderResponse ->
                SearchNpgOperationsResponseDto().apply {
                    operations =
                        orderResponse.operations
                            ?.map { operation ->
                                OperationDto().apply {
                                    // Extract authorizationCode and rrn if they exist
                                    logger.debug(
                                        "NPG operation additionalData: {}",
                                        operation.additionalData
                                    )
                                    additionalData =
                                        OperationAdditionalDataDto().apply {
                                            authorizationCode =
                                                operation.additionalData
                                                    ?.get("authorizationCode")
                                                    ?.toString()
                                            rrn = operation.additionalData?.get("rrn")?.toString()
                                        }

                                    operationAmount = operation.operationAmount
                                    operationCurrency = operation.operationCurrency
                                    operationId = operation.operationId
                                    operationResult =
                                        OperationResultDto.valueOf(
                                            operation.operationResult.toString()
                                        )
                                    operationTime = operation.operationTime
                                    operationType =
                                        OperationTypeDto.valueOf(operation.operationType.toString())
                                    orderId = operation.orderId
                                    paymentCircuit = operation.paymentCircuit
                                    paymentEndToEndId = operation.paymentEndToEndId
                                    paymentMethod =
                                        operation.paymentMethod?.let {
                                            PaymentMethodDto.valueOf(it.toString())
                                        }
                                }
                            }
                            ?.toList()
                }
            }
            .doOnNext { orderResponse ->
                logger.info(
                    "Retrieved NPG operations for transaction [{}]: found {} operations",
                    transactionId,
                    orderResponse.operations?.size ?: 0
                )
            }
    }

    private fun retrieveTransactionDetails(
        transactionId: String
    ): Mono<NTuple4<String, String, String, PaymentMethod>> {
        return Mono.just(transactionId)
            .flatMapMany {
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transactionId
                )
            }
            .reduce(
                it.pagopa.ecommerce.commons.domain.v2.EmptyTransaction(),
                it.pagopa.ecommerce.commons.domain.v2.Transaction::applyEvent
            )
            .cast(BaseTransaction::class.java)
            .map { baseTransaction ->
                val transactionActivatedData =
                    if (baseTransaction is BaseTransactionWithPaymentToken) {
                        baseTransaction.transactionActivatedData
                    } else null

                val authRequestData =
                    when (baseTransaction) {
                        is BaseTransactionWithRequestedAuthorization ->
                            baseTransaction.transactionAuthorizationRequestData
                        else -> null
                    }

                val correlationId =
                    (transactionActivatedData?.transactionGatewayActivationData
                            as? NpgTransactionGatewayActivationData) // also contains orderId
                        ?.correlationId
                        ?: throw NoOperationDataFoundException(
                            "No correlation ID found for transaction $transactionId"
                        )
                NTuple4(
                    authRequestData?.authorizationRequestId
                        ?: throw NoOperationDataFoundException(
                            "No authorization request ID found for transaction $transactionId"
                        ),
                    authRequestData.pspId
                        ?: throw NoOperationDataFoundException(
                            "No PSP ID found for transaction $transactionId"
                        ),
                    correlationId,
                    PaymentMethod.valueOf(
                        authRequestData.paymentMethodName
                            ?: throw NoOperationDataFoundException(
                                "No payment method found for transaction $transactionId"
                            )
                    )
                )
            }
    }

    /**
     * Imported from transactions-scheduler.
     *
     * @see <a
     *   href="https://github.com/pagopa/pagopa-ecommerce-transactions-scheduler-service/blob/b9ed99fbf6724ba04e646b7b490a43c69c453ebc/src/main/kotlin/it/pagopa/ecommerce/transactions/scheduler/services/TransactionInfoService.kt#L248">performGetOrderNP
     *   on transactions-scheduler</a>
     */
    private fun performGetOrderNPG(
        transactionId: TransactionId,
        orderId: String,
        pspId: String,
        correlationId: String,
        paymentMethod: PaymentMethod
    ): Mono<OrderResponseDto> {
        logger.info(
            "Performing get order for transaction with id: [{}], orderId [{}], pspId: [{}], correlationId: [{}], paymentMethod: [{}]",
            transactionId.value(),
            orderId,
            pspId,
            correlationId,
            paymentMethod.serviceName,
        )
        return npgApiKeyConfiguration[paymentMethod, pspId].fold(
            { ex -> Mono.error(ex) },
            { apiKey ->
                npgClient.getOrder(UUID.fromString(correlationId), apiKey, orderId).onErrorMap(
                    NpgResponseException::class.java
                ) { exception: NpgResponseException ->
                    val responseStatusCode = exception.statusCode
                    responseStatusCode
                        .map {
                            if (it.is5xxServerError) {
                                NpgBadGatewayException("$it")
                            } else {
                                NpgBadRequestException(transactionId.value(), "$it")
                            }
                        }
                        .orElse(exception)
                }
            }
        )
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

data class NTuple4<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)
