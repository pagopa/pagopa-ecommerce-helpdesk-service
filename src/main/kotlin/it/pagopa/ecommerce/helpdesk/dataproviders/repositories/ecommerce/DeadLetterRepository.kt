package it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce

import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/** DeadLetterEvent repository */
@Repository
interface DeadLetterRepository : ReactiveCrudRepository<DeadLetterEvent, String> {

    @Query("{'insertionDate': {'\$gte': '?0','\$lte': '?1'}}", count = true)
    fun countAllDeadLetterEventInTimeRange(startTime: String, endTime: String): Mono<Long>

    @Query("{'insertionDate': {'\$gte': '?0','\$lte': '?1'}, 'transactionInfo.eCommerceStatus': {'\$nin': ?2}, 'transactionInfo.details.operationResult': {'\$nin': ?3} }", count = true)
    fun countAllDeadLetterEventInTimeRangeWithExludeStatuses(startTime: String, endTime: String, ecommerceStatusesToExclude: List<String>, npgStatusesToExclude: List<String>): Mono<Long>

    @Query("{'transactionInfo.eCommerceStatus': {'\$nin': ?2}, 'transactionInfo.details.operationResult': {'\$nin': ?3} }", count = true)
    fun countAllDeadLetterEventWithExludeStatuses(ecommerceStatusesToExclude: List<String>, npgStatusesToExclude: List<String>): Mono<Long>

    @Query("{'queueName': '?0'}", count = true)
    fun countDeadLetterEventForQueue(queueName: String): Mono<Long>

    @Query("{'insertionDate': {'\$gte': '?1','\$lte': '?2'},'queueName': '?0'}", count = true)
    fun countDeadLetterEventForQueueInTimeRange(
        queueName: String,
        startTime: String,
        endTime: String
    ): Mono<Long>

    @Query("{'insertionDate': {'\$gte': '?1','\$lte': '?2'},'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?3}, 'transactionInfo.details.operationResult': {'\$nin': ?4}}", count = true)
    fun countDeadLetterEventForQueueInTimeRangeWithExludeStatuses(
        queueName: String,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: List<String>,
        npgStatusesToExclude: List<String>
    ): Mono<Long>

    @Query("{'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?3}, 'transactionInfo.details.operationResult': {'\$nin': ?4}}", count = true)
    fun countDeadLetterEventForQueueWithExludeStatuses(
        queueName: String,
        ecommerceStatusesToExclude: List<String>,
        npgStatusesToExclude: List<String>
    ): Mono<Long>

    @Aggregation(
        "{\$match: {'queueName': '?0'}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(
        queueName: String,
        skip: Int,
        limit: Int,
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: {'insertionDate': {'\$gte': '?3','\$lte': '?4'},'queueName': '?0'}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRange(
        queueName: String,
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: {'insertionDate': {'\$gte': '?3','\$lte': '?4'},'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?5}, 'transactionInfo.details.operationResult': {'\$nin': ?6}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
        queueName: String,
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: List<String>,
        npgStatusesToExclude: List<String>
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: {'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?3}, 'transactionInfo.details.operationResult': {'\$nin': ?4}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescWithExludeStatuses(
        queueName: String,
        skip: Int,
        limit: Int,
        ecommerceStatusesToExclude: List<String>,
        npgStatusesToExclude: List<String>
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}",
    )
    fun findDeadLetterEventPaginatedOrderByInsertionDateDesc(
        skip: Int,
        limit: Int,
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: {'insertionDate': {'\$gte': '?2','\$lte': '?3'}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}",
    )
    fun findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRange(
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: {'insertionDate': {'\$gte': '?2','\$lte': '?3'}, 'transactionInfo.eCommerceStatus': {'\$nin': ?4}, 'transactionInfo.details.operationResult': {'\$nin': ?5}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}",
    )
    fun findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExludeStatuses(
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: List<String>,
        npgStatusesToExclude: List<String>
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: {'transactionInfo.eCommerceStatus': {'\$nin': ?4}, 'transactionInfo.details.operationResult': {'\$nin': ?5}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}",
    )
    fun findDeadLetterEventPaginatedOrderByInsertionDateDescWithExludeStatuses(
        skip: Int,
        limit: Int,
        ecommerceStatusesToExclude: List<String>,
        npgStatusesToExclude: List<String>
    ): Flux<DeadLetterEvent>

    @Aggregation(
        "{\$match: { 'insertionDate': {'\$gte': '?2','\$lte': '?3'}, 'transactionInfo.details.operationResult': {'\$nin': ?4},'transactionInfo.eCommerceStatus': {'\$nin': ?5} }}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}"
    )
    fun findDeadLetterEventWithExclusions(
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        npgStatusToExclude: List<String>,
        ecommerceStatusToExclude: List<String>
    ): Flux<DeadLetterEvent>
}
