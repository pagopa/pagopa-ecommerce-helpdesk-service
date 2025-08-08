package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.client.NpgClient.PaymentMethod
import it.pagopa.ecommerce.commons.domain.v2.TransactionId
import it.pagopa.ecommerce.commons.exceptions.NpgResponseException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.DeadLetterDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider as EcommerceTransactionDataProviderV1
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider as EcommerceTransactionDataProviderV2
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadGatewayException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadRequestException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialFiscalCodeUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildDeadLetterEventsSearchResponse
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
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
import jakarta.enterprise.context.ApplicationScoped
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestTransactionIdDto as SearchTransactionRequestTransactionIdDtoV2
import java.util.*
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import jakarta.inject.Inject
import jakarta.inject.Named
import org.springframework.beans.factory.annotation.Qualifier
import reactor.core.publisher.Mono

@ApplicationScoped
@Named("EcommerceServiceV1")
class EcommerceService(
    @Inject private val ecommerceTransactionDataProvider: EcommerceTransactionDataProviderV1,
    @Inject private val ecommerceTransactionDataProviderV2: EcommerceTransactionDataProviderV2,
    @Inject private val deadLetterDataProvider: DeadLetterDataProvider,
    @Inject
    @Named("confidential-data-manager-client-email")
    private val confidentialDataManager: ConfidentialDataManager,
    @Inject val npgClient: NpgClient,
    @Inject val npgApiKeyConfiguration: NpgApiKeyConfiguration
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): SearchTransactionResponseDto {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return searchPaginatedResult(
            pageNumber = pageNumber,
            pageSize = pageSize,
            searchCriteria = SearchParamDecoder(
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
    ): SearchDeadLetterEventResponseDto {
        logger.info(
            "[helpDesk ecommerce service] search dead letter events, type: {}",
            searchRequest.source
        )

        val timeRange: DeadLetterSearchDateTimeRangeDto? = searchRequest.timeRange

        if (timeRange != null && timeRange.startDate >= timeRange.endDate) {
            throw InvalidSearchCriteriaException(
                "Invalid time range: startDate [${timeRange.startDate}] is not greater than endDate: [${timeRange.endDate}]"
            )
        }

        val (results, totalCount) = searchPaginatedResult(
            pageNumber = pageNumber,
            pageSize = pageSize,
            searchCriteria = searchRequest,
            searchCriteriaType = searchRequest.toString(),
            dataProvider = deadLetterDataProvider
        )

        return buildDeadLetterEventsSearchResponse(
            currentPage = pageNumber,
            totalCount = totalCount,
            pageSize = pageSize,
            results = results
        )
    }

    /**
     * Searches for NPG operations associated with a given transaction ID.
     *
     * The search process follows these steps:
     * 1. Searches for the transaction using [EcommerceTransactionDataProviderV2.findResult]
     * 2. If found and has a valid correlationId, retrieves NPG order details using
     *    [performGetOrderNPG]
     * 3. Maps the NPG order response to a [SearchNpgOperationsResponseDto] containing operation
     *    details
     *
     * @param transactionId The ID of the transaction to search for
     * @return Mono<SearchNpgOperationsResponseDto> containing the NPG operations data, or
     *   NoResultFoundException if the transaction is not found or lacks required data
     */
    fun searchNpgOperations(transactionId: String): SearchNpgOperationsResponseDto {
        val searchCriteria =
            SearchParamDecoderV2(
                searchParameter =
                    SearchTransactionRequestTransactionIdDtoV2().apply {
                        this.transactionId = transactionId
                        this.type = "TRANSACTION_ID"
                    },
                confidentialMailUtils =
                    it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils(
                        confidentialDataManager
                    ),
                confidentialFiscalCodeUtils = ConfidentialFiscalCodeUtils(confidentialDataManager)
            )

        return ecommerceTransactionDataProviderV2.findResult(searchCriteria, 0, 1).flatMap { list ->
            val optionalResult = list.firstOrNull()?.let { Optional.of(it) } ?: Optional.empty()
            if (
                optionalResult.isPresent &&
                    optionalResult.get().transactionInfo?.correlationId != null
            ) {
                performGetOrderNPG(
                        transactionId = TransactionId(transactionId),
                        orderId = optionalResult.get().transactionInfo.authorizationRequestId,
                        pspId = optionalResult.get().pspInfo.pspId,
                        correlationId =
                            optionalResult.get().transactionInfo.correlationId.toString(),
                        paymentMethod =
                            NpgClient.PaymentMethod.valueOf(
                                optionalResult.get().transactionInfo.paymentMethodName
                            )
                    )
                    .map { orderResponse ->
                        SearchNpgOperationsResponseDto().apply {
                            operations =
                                orderResponse.operations
                                    ?.map { operation ->
                                        OperationDto().apply {
                                            additionalData =
                                                OperationAdditionalDataDto().apply {
                                                    authorizationCode =
                                                        operation.additionalData
                                                            ?.get("authorizationCode")
                                                            ?.toString()
                                                    rrn =
                                                        operation.additionalData
                                                            ?.get("rrn")
                                                            ?.toString()
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
                                                OperationTypeDto.valueOf(
                                                    operation.operationType.toString()
                                                )
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
            } else {
                Mono.error(NoResultFoundException(transactionId))
            }
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
    ): Pair<List<V>, Int> {
        val totalCount = dataProvider.totalRecordCount(searchCriteria)

        if (totalCount > 0) {
            val skip = pageSize * pageNumber

            logger.info(
                "Total record found: {}, skip: {}, limit: {}",
                totalCount,
                skip,
                pageSize
            )

            val results = dataProvider.findResult(
                searchParams = searchCriteria,
                skip = skip,
                limit = pageSize
            )

            return Pair(results, totalCount)
        } else {
            throw NoResultFoundException(searchCriteriaType)
        }
    }
}
