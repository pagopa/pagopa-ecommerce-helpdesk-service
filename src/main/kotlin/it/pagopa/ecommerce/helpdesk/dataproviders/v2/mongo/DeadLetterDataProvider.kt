package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import it.pagopa.ecommerce.helpdesk.dataproviders.DeadLetterRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.DataProvider
import it.pagopa.generated.ecommerce.helpdesk.v2.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.DeadLetterSearchEventSourceDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchDeadLetterEventsRequestDto
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
@Component("DeadLetterDataProviderV2")
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
        val source = searchParams.source!!
        val timeRange = searchParams.timeRange
        return when (source) {
            DeadLetterSearchEventSourceDto.ALL -> {
                if (timeRange != null) {
                    val startDate = timeRange.startDate.toString()
                    val endDate = timeRange.endDate.toString()
                    logger.info(
                        "Counting all dead letter events in time range {} - {}",
                        startDate,
                        endDate
                    )
                    deadLetterRepository.countAllDeadLetterEventInTimeRange(
                        startTime = startDate,
                        endTime = endDate
                    )
                } else {
                    logger.info("Counting all dead letter events")
                    deadLetterRepository.count()
                }
            }
            DeadLetterSearchEventSourceDto.ECOMMERCE,
            DeadLetterSearchEventSourceDto.NOTIFICATIONS_SERVICE -> {
                if (timeRange != null) {
                    val queueName = deadLetterQueueMapping[source]!!
                    val startDate = timeRange.startDate.toString()
                    val endDate = timeRange.endDate.toString()
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
                } else {
                    val queueName = deadLetterQueueMapping[source]!!
                    logger.info("Counting all dead letter events for queue {}", queueName)
                    deadLetterRepository.countDeadLetterEventForQueue(queueName)
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
        val source = searchParams.source!!
        val timeRange = searchParams.timeRange
        return when (source) {
                DeadLetterSearchEventSourceDto.ALL -> {
                    if (timeRange != null) {
                        val startDate = timeRange.startDate.toString()
                        val endDate = timeRange.endDate.toString()
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
                    } else {
                        logger.info("Finding all dead letter events")
                        deadLetterRepository.findDeadLetterEventPaginatedOrderByInsertionDateDesc(
                            skip = skip,
                            limit = limit
                        )
                    }
                }
                DeadLetterSearchEventSourceDto.ECOMMERCE,
                DeadLetterSearchEventSourceDto.NOTIFICATIONS_SERVICE -> {
                    val queueName = deadLetterQueueMapping[source]!!
                    if (timeRange != null) {
                        val startDate = timeRange.startDate.toString()
                        val endDate = timeRange.endDate.toString()
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
                    } else {
                        logger.info("Finding all dead letter events for queue name {}", queueName)
                        deadLetterRepository
                            .findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(
                                queueName = queueName,
                                skip = skip,
                                limit = limit
                            )
                    }
                }
            }
            .map { mapToDeadLetterEventDto(it) }
            .collectList()
    }

    private fun mapToDeadLetterEventDto(deadLetterEvent: DeadLetterEvent): DeadLetterEventDto =
        DeadLetterEventDto()
            .data(deadLetterEvent.data)
            .queueName(deadLetterEvent.queueName)
            .timestamp(OffsetDateTime.parse(deadLetterEvent.insertionDate))
}
