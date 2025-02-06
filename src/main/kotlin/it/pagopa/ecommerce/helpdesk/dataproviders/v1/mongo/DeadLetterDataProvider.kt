package it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo

import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import it.pagopa.ecommerce.commons.documents.v2.deadletter.DeadLetterNpgTransactionInfoDetailsData
import it.pagopa.ecommerce.commons.documents.v2.deadletter.DeadLetterRedirectTransactionInfoDetailsData
import it.pagopa.ecommerce.commons.documents.v2.deadletter.DeadLetterTransactionInfo
import it.pagopa.ecommerce.commons.documents.v2.deadletter.DeadLetterTransactionInfoDetailsData
import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.DeadLetterRepository
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Dead letter data provider. This interface models common function for transaction data provider in
 * order to search for transactions given a specific DeadLetterEventSourceDto criteria
 *
 * @see DataProvider
 * @see DeadLetterEventDto
 * @see DeadLetterEventDto
 */
@Component
class DeadLetterDataProvider(
    @Autowired private val deadLetterRepository: DeadLetterRepository,
    @Autowired private val deadLetterQueueMapping: EnumMap<DeadLetterSearchEventSourceDto, String>
) : DataProvider<EcommerceSearchDeadLetterEventsRequestDto, DeadLetterEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    /*
     * @formatter:off
     *
     * Warning kotlin:S6611 - Map values should be accessed safely
     * Suppressed because deadLetterQueueMapping enum map content is validated
     * at bean construction time checking that all DeadLetterSearchEventSourceDto enumeration
     * are mapped to a valid string
     *
     * @formatter:on
     */
    @SuppressWarnings("kotlin:S6611")
    override fun totalRecordCount(
        searchParams: EcommerceSearchDeadLetterEventsRequestDto
    ): Mono<Int> {
        val source = searchParams.source ?: return Mono.just(0)
        val timeRange = searchParams.timeRange
        val excludeStatuses = searchParams.exludeStatuses

        val startDate = timeRange?.startDate?.toString() ?: ""
        val endDate = timeRange?.endDate?.toString() ?: ""
        val npgStatuses = excludeStatuses?.npsStatus ?: emptyList()
        val eCommerceStatuses = excludeStatuses?.ecommerceStatus ?: emptyList()

        return when (source) {
            DeadLetterSearchEventSourceDto.ALL -> {
                when {
                    excludeStatuses != null && timeRange != null -> {
                        logger.info(
                            "Counting all dead letter events in time range {} - {} with eCommerceStatus not in {} and npgStatus not in {}",
                            startDate,
                            endDate,
                            npgStatuses.toString(),
                            eCommerceStatuses.toString()
                        )
                        deadLetterRepository.countAllDeadLetterEventInTimeRangeWithExludeStatuses(
                            startTime = startDate,
                            endTime = endDate,
                            ecommerceStatusesToExclude = eCommerceStatuses,
                            npgStatusesToExclude = npgStatuses
                        )
                    }
                    excludeStatuses == null && timeRange != null -> {
                        logger.info(
                            "Counting all dead letter events in time range {} - {}",
                            startDate,
                            endDate
                        )
                        deadLetterRepository.countAllDeadLetterEventInTimeRange(
                            startTime = startDate,
                            endTime = endDate
                        )
                    }
                    excludeStatuses != null -> {
                        logger.info(
                            "Counting all dead letter events with eCommerceStatus not in {} and npgStatus not in {}",
                            npgStatuses.toString(),
                            eCommerceStatuses.toString()
                        )
                        deadLetterRepository.countAllDeadLetterEventWithExludeStatuses(
                            ecommerceStatusesToExclude = eCommerceStatuses,
                            npgStatusesToExclude = npgStatuses
                        )
                    }
                    else -> {
                        logger.info("Counting all dead letter events")
                        deadLetterRepository.count()
                    }
                }
            }
            DeadLetterSearchEventSourceDto.ECOMMERCE,
            DeadLetterSearchEventSourceDto.NOTIFICATIONS_SERVICE -> {
                val queueName = deadLetterQueueMapping[source]!!
                when {
                    excludeStatuses != null && timeRange != null -> {
                        logger.info(
                            "Counting all dead letter events for queue {} in time range {} - {} with eCommerceStatus not in {} and npgStatus not in {}",
                            queueName,
                            startDate,
                            endDate,
                            eCommerceStatuses.toString(),
                            npgStatuses.toString()
                        )
                        deadLetterRepository
                            .countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
                                queueName = queueName,
                                startTime = startDate,
                                endTime = endDate,
                                ecommerceStatusesToExclude = eCommerceStatuses,
                                npgStatusesToExclude = npgStatuses
                            )
                    }
                    excludeStatuses == null && timeRange != null -> {
                        logger.info(
                            "Counting all dead letter events for queue {} in time range {} - {}",
                            queueName,
                            startDate,
                            endDate
                        )
                        deadLetterRepository.countDeadLetterEventForQueueInTimeRange(
                            queueName = queueName,
                            startTime = startDate,
                            endTime = endDate
                        )
                    }
                    excludeStatuses != null -> {
                        logger.info(
                            "Counting all dead letter events for queue {} with eCommerceStatus not in {} and npgStatus not in {}",
                            queueName,
                            eCommerceStatuses.toString(),
                            npgStatuses.toString()
                        )
                        deadLetterRepository.countDeadLetterEventForQueueWithExludeStatuses(
                            queueName = queueName,
                            ecommerceStatusesToExclude = eCommerceStatuses,
                            npgStatusesToExclude = npgStatuses
                        )
                    }
                    else -> {
                        logger.info("Counting all dead letter events for queue {}", queueName)
                        deadLetterRepository.countDeadLetterEventForQueue(queueName)
                    }
                }
            }
        }.map { it.toInt() }
    }

    /*
     * @formatter:off
     *
     * Warning kotlin:S6611 - Map values should be accessed safely
     * Suppressed because deadLetterQueueMapping enum map content is validated
     * at bean construction time checking that all DeadLetterSearchEventSourceDto enumeration
     * are mapped to a valid string
     *
     * @formatter:on
     */
    @SuppressWarnings("kotlin:S6611")
    override fun findResult(
        searchParams: EcommerceSearchDeadLetterEventsRequestDto,
        skip: Int,
        limit: Int
    ): Mono<List<DeadLetterEventDto>> {
        val source = searchParams.source ?: return Mono.just(emptyList())
        val timeRange = searchParams.timeRange
        val excludeStatuses = searchParams.exludeStatuses

        val startDate = timeRange?.startDate?.toString() ?: ""
        val endDate = timeRange?.endDate?.toString() ?: ""
        val npgStatuses = excludeStatuses?.npsStatus ?: emptyList()
        val eCommerceStatuses = excludeStatuses?.ecommerceStatus ?: emptyList()

        return when (source) {
                DeadLetterSearchEventSourceDto.ALL -> {
                    when {
                        excludeStatuses != null && timeRange != null -> {
                            logger.info(
                                "Finding all dead letter events in time range {} - {} with eCommerceStatus not in {} and npgStatus not in {}",
                                startDate,
                                endDate,
                                eCommerceStatuses.toString(),
                                npgStatuses.toString()
                            )
                            deadLetterRepository
                                .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                                    skip = skip,
                                    limit = limit,
                                    startTime = startDate,
                                    endTime = endDate,
                                    ecommerceStatusesToExclude = eCommerceStatuses,
                                    npgStatusesToExclude = npgStatuses
                                )
                        }
                        excludeStatuses == null && timeRange != null -> {
                            logger.info(
                                "Finding all dead letter events in time range {} - {}",
                                startDate,
                                endDate
                            )
                            deadLetterRepository
                                .findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
                                    skip = skip,
                                    limit = limit,
                                    startTime = startDate,
                                    endTime = endDate
                                )
                        }
                        excludeStatuses != null -> {
                            logger.info(
                                "Finding all dead letter events with eCommerceStatus not in {} and npgStatus not in {}",
                                eCommerceStatuses.toString(),
                                npgStatuses.toString()
                            )
                            deadLetterRepository
                                .findDeadLetterEventPaginatedOrderByInsertionDateDescWithExludeStatuses(
                                    skip = skip,
                                    limit = limit,
                                    ecommerceStatusesToExclude = eCommerceStatuses,
                                    npgStatusesToExclude = npgStatuses
                                )
                        }
                        else -> {
                            logger.info("Finding all dead letter events")
                            deadLetterRepository
                                .findDeadLetterEventPaginatedOrderByInsertionDateDesc(
                                    skip = skip,
                                    limit = limit
                                )
                        }
                    }
                }
                DeadLetterSearchEventSourceDto.ECOMMERCE,
                DeadLetterSearchEventSourceDto.NOTIFICATIONS_SERVICE -> {
                    val queueName = deadLetterQueueMapping[source]!!

                    when {
                        excludeStatuses != null && timeRange != null -> {
                            logger.info(
                                "Finding all dead letter events for queue name {} in time range {} - {} with eCommerceStatus not in {} and npgStatus not in {}",
                                queueName,
                                startDate,
                                endDate,
                                eCommerceStatuses.toString(),
                                npgStatuses.toString()
                            )
                            deadLetterRepository
                                .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
                                    queueName = queueName,
                                    skip = skip,
                                    limit = limit,
                                    startTime = startDate,
                                    endTime = endDate,
                                    ecommerceStatusesToExclude = eCommerceStatuses,
                                    npgStatusesToExclude = npgStatuses
                                )
                        }
                        excludeStatuses == null && timeRange != null -> {
                            logger.info(
                                "Finding all dead letter events for queue name {} in time range {} - {}",
                                queueName,
                                startDate,
                                endDate
                            )
                            deadLetterRepository
                                .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
                                    queueName = queueName,
                                    skip = skip,
                                    limit = limit,
                                    startTime = startDate,
                                    endTime = endDate
                                )
                        }
                        excludeStatuses != null -> {
                            logger.info(
                                "Finding all dead letter events for queue name {} with eCommerceStatus not in {} and npgStatus not in {}",
                                queueName,
                                eCommerceStatuses.toString(),
                                npgStatuses.toString()
                            )
                            deadLetterRepository
                                .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescWithExludeStatuses(
                                    queueName = queueName,
                                    skip = skip,
                                    limit = limit,
                                    ecommerceStatusesToExclude = eCommerceStatuses,
                                    npgStatusesToExclude = npgStatuses
                                )
                        }
                        else -> {
                            logger.info(
                                "Finding all dead letter events for queue name {}",
                                queueName
                            )
                            deadLetterRepository
                                .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(
                                    queueName = queueName,
                                    skip = skip,
                                    limit = limit
                                )
                        }
                    }
                }
            }
            .map { mapToDeadLetterEventDto(it) }
            .collectList()
    }

    fun mapToDeadLetterEventDto(deadLetterEvent: DeadLetterEvent): DeadLetterEventDto =
        DeadLetterEventDto()
            .data(deadLetterEvent.data)
            .queueName(deadLetterEvent.queueName)
            .timestamp(OffsetDateTime.parse(deadLetterEvent.insertionDate))
            .transactionInfo(deadLetterEvent.transactionInfo?.let { mapToTransactionInfoDto(it) })

    private fun mapToTransactionInfoDto(
        transactionInfo: DeadLetterTransactionInfo
    ): DeadLetterTransactionInfoDto =
        DeadLetterTransactionInfoDto()
            .transactionId(transactionInfo.transactionId)
            .authorizationRequestId(transactionInfo.authorizationRequestId)
            .eCommerceStatus(
                transactionInfo.eCommerceStatus?.let { TransactionStatusDto.valueOf(it.value) }
            )
            .paymentGateway(transactionInfo.gateway?.name)
            .paymentTokens(transactionInfo.paymentTokens)
            .pspId(transactionInfo.pspId)
            .paymentMethodName(transactionInfo.paymentMethodName)
            .grandTotal(transactionInfo.grandTotal)
            .rrn(transactionInfo.rrn)
            .details(mapToTransactionInfoDetailsDto(transactionInfo.details))

    private fun mapToTransactionInfoDetailsDto(
        details: DeadLetterTransactionInfoDetailsData?
    ): DeadLetterTransactionInfoDetailsDto? =
        when (details) {
            is DeadLetterNpgTransactionInfoDetailsData ->
                NpgTransactionInfoDetailsDataDto()
                    .type(details.type.toString())
                    .operationId(details.operationId)
                    .operationResult(details.operationResult?.value)
                    .correlationId(details.correlationId?.let { UUID.fromString(it) })
                    .paymentEndToEndId(details.paymentEndToEndId)
            is DeadLetterRedirectTransactionInfoDetailsData ->
                RedirectTransactionInfoDetailsDataDto()
                    .type(details.type.toString())
                    .outcome(details.outcome)
            else -> null
        }
}
