package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.client.NpgClient.PaymentMethod
import it.pagopa.ecommerce.commons.domain.TransactionId
import it.pagopa.ecommerce.commons.exceptions.NpgResponseException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadGatewayException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadRequestException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildDeadLetterEventsSearchResponse
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchDateTimeRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchDeadLetterEventsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationAdditionalDataDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationTypeDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentMethodDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.util.*
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("EcommerceServiceV1")
class EcommerceService(
    @Autowired private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider,
    @Autowired private val deadLetterDataProvider: DeadLetterDataProvider,
    @Autowired private val confidentialDataManager: ConfidentialDataManager,
    @Autowired val npgClient: NpgClient,
    @Autowired val npgApiKeyConfiguration: NpgApiKeyConfiguration
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return searchPaginatedResult(
                pageNumber = pageNumber,
                pageSize = pageSize,
                searchCriteria =
                    SearchParamDecoder(
                        searchParameter = ecommerceSearchTransactionRequestDto,
                        confidentialMailUtils = ConfidentialMailUtils(confidentialDataManager)
                    ),
                searchCriteriaType = ecommerceSearchTransactionRequestDto.type,
                dataProvider = ecommerceTransactionDataProvider
            )
            .map { (results, totalCount) ->
                buildTransactionSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalCount,
                    pageSize = pageSize,
                    results = results
                )
            }
    }

    fun searchDeadLetterEvents(
        pageNumber: Int,
        pageSize: Int,
        searchRequest: EcommerceSearchDeadLetterEventsRequestDto
    ): Mono<SearchDeadLetterEventResponseDto> {
        logger.info(
            "[helpDesk ecommerce service] search dead letter events, type: {}",
            searchRequest.source
        )
        val timeRange: DeadLetterSearchDateTimeRangeDto? = searchRequest.timeRange
        return mono { searchRequest }
            .filter { timeRange == null || timeRange.startDate < timeRange.endDate }
            .switchIfEmpty(
                Mono.error(
                    InvalidSearchCriteriaException(
                        "Invalid time range: startDate [${timeRange?.startDate}] is not greater than endDate: [${timeRange?.endDate}]"
                    )
                )
            )
            .flatMap {
                searchPaginatedResult(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchCriteria = searchRequest,
                    searchCriteriaType = it.toString(),
                    dataProvider = deadLetterDataProvider
                )
            }
            .map { (results, totalCount) ->
                buildDeadLetterEventsSearchResponse(
                    currentPage = pageNumber,
                    totalCount = totalCount,
                    pageSize = pageSize,
                    results = results
                )
            }
    }

    /**
     * Used by the API that searches for NPG operations. Starting from the transaction id, first it
     * retrieves the transaction details calling the
     * [EcommerceTransactionDataProvider.retrieveTransactionDetails] that returns an object of the
     * data class [NTuple4], then it uses the details to call the [performGetOrderNPG]. Returns a
     * [SearchNpgOperationsResponseDto], representing a subset of [OrderResponseDto] data.
     */
    fun searchNpgOperations(transactionId: String): Mono<SearchNpgOperationsResponseDto> {
        return ecommerceTransactionDataProvider
            .retrieveTransactionDetails(transactionId)
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

    private fun <K, V> searchPaginatedResult(
        pageNumber: Int,
        pageSize: Int,
        searchCriteria: K,
        dataProvider: DataProvider<K, V>,
        searchCriteriaType: String
    ): Mono<Pair<List<V>, Int>> {
        return dataProvider.totalRecordCount(searchCriteria).flatMap { totalCount ->
            if (totalCount > 0) {
                val skip = pageSize * pageNumber
                logger.info(
                    "Total record found: {}, skip: {}, limit: {}",
                    totalCount,
                    skip,
                    pageSize
                )
                dataProvider
                    .findResult(searchParams = searchCriteria, skip = skip, limit = pageSize)
                    .zipWith(mono { totalCount }, ::Pair)
            } else {
                Mono.error(NoResultFoundException(searchCriteriaType))
            }
        }
    }
}

data class NTuple4<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)
