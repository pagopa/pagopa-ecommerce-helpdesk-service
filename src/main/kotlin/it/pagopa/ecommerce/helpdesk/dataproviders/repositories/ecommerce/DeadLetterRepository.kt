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

    @Query(
        "{'insertionDate': {'\$gte': '?0','\$lte': '?1'}, 'transactionInfo.eCommerceStatus': {'\$nin': ?2}, 'transactionInfo.details.operationResult': {'\$nin': ?3} }",
        count = true
    )
    fun countAllDeadLetterEventInTimeRangeWithExcludeStatuses(
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Mono<Long>

    @Query("{'queueName': '?0'}", count = true)
    fun countDeadLetterEventForQueue(queueName: String): Mono<Long>

    @Query(
        "{'insertionDate': {'\$gte': '?1','\$lte': '?2'},'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?3}, 'transactionInfo.details.operationResult': {'\$nin': ?4}}",
        count = true
    )
    fun countDeadLetterEventForQueueInTimeRangeWithExcludeStatuses(
        queueName: String,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
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
        "{\$match: {'insertionDate': {'\$gte': '?3','\$lte': '?4'},'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?5}, 'transactionInfo.details.operationResult': {'\$nin': ?6}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatuses(
        queueName: String,
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
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
        "{\$match: {'insertionDate': {'\$gte': '?2','\$lte': '?3'}, 'transactionInfo.eCommerceStatus': {'\$nin': ?4}, 'transactionInfo.details.operationResult': {'\$nin': ?5}}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}",
    )
    fun findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatuses(
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Flux<DeadLetterEvent>
}
