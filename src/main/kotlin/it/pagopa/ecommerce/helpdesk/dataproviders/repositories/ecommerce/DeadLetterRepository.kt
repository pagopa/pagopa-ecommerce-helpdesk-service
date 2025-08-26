package it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce

import com.mongodb.client.model.Aggregates.*
import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClient
import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import it.pagopa.ecommerce.helpdesk.configurations.MongoClientsProvider
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.bson.conversions.Bson
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import com.mongodb.client.model.Filters.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import com.mongodb.client.model.Filters.eq
import io.smallrye.mutiny.Uni
import org.bson.Document
import org.reactivestreams.Publisher

/** DeadLetterEvent repository */
@ApplicationScoped
class DeadLetterRepository {

    @Inject
    lateinit var mongoClient: MongoClient

    @Inject
    lateinit var mongoClientsProvider: MongoClientsProvider

    private val collectionName = "deadLetterEvent"


    private fun getCollection() =
        mongoClient.getDatabase(mongoClientsProvider.getDefaultDatabaseName())
            .getCollection(collectionName, DeadLetterEvent::class.java)
    /*@Query(
        "{'insertionDate': {'\$gte': '?0','\$lte': '?1'}, 'transactionInfo.eCommerceStatus': {'\$nin': ?2}, 'transactionInfo.details.operationResult': {'\$nin': ?3} }",
        count = true
    )
    fun countAllDeadLetterEventInTimeRangeWithExcludeStatuses(
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Mono<Long>*/
    fun countAllDeadLetterEventInTimeRangeWithExcludeStatuses(
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Mono<Long> {
      
        val filter: Bson = and(
            gte("insertionDate", parseDate(startTime)),
            lte("insertionDate", parseDate(endTime)),
            nin("transactionInfo.eCommerceStatus", ecommerceStatusesToExclude),
            nin("transactionInfo.details.operationResult", npgStatusesToExclude)
        )

        return Mono.from(getCollection().countDocuments(filter))
    }



   /* @Query("{'queueName': '?0'}", count = true)*/
   //fun countDeadLetterEventForQueue(queueName: String): Mono<Long>
   fun countDeadLetterEventForQueue(queueName: String): Mono<Long> {
      
        val filter = Filters.eq("queueName", queueName)

        return Mono.from(getCollection().countDocuments(filter))
   }

    /*@Query(
        "{'insertionDate': {'\$gte': '?1','\$lte': '?2'},'queueName': '?0', 'transactionInfo.eCommerceStatus': {'\$nin': ?3}, 'transactionInfo.details.operationResult': {'\$nin': ?4}}",
        count = true
    )
    fun countDeadLetterEventForQueueInTimeRangeWithExcludeStatuses(
        queueName: String,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Mono<Long>*/

    fun countDeadLetterEventForQueueInTimeRangeWithExcludeStatuses(
        queueName: String,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Mono<Long> {
      
        val filter: Bson = and(
            eq("queueName", queueName),
            gte("insertionDate", parseDate(startTime)),
            lte("insertionDate", parseDate(endTime)),
            nin("transactionInfo.eCommerceStatus", ecommerceStatusesToExclude),
            nin("transactionInfo.details.operationResult", npgStatusesToExclude)
        )

        return Mono.from(getCollection().countDocuments(filter))
    }

    /*@Aggregation(
        "{\$match: {'queueName': '?0'}}",
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(
        queueName: String,
        skip: Int,
        limit: Int,
    ): Flux<DeadLetterEvent>*/
    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDesc(
        queueName: String,
        skip: Int,
        limit: Int
    ): Flux<DeadLetterEvent> {
      
        val pipeline = listOf(
            match(eq("queueName", queueName)),
            sort(Document("insertionDate", -1)),
            skip(skip),
            limit(limit)
        )

        return Flux.from(getCollection().aggregate(pipeline))
    }



    /*@Aggregation(
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
    ): Flux<DeadLetterEvent>*/

    fun findDeadLetterEventForQueuePaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatuses(
        queueName: String,
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Flux<DeadLetterEvent> {
      
        val pipeline = listOf(
            match(
                and(
                    eq("queueName", queueName),
                    gte("insertionDate", startTime),
                    lte("insertionDate", endTime),
                    nin("transactionInfo.eCommerceStatus", ecommerceStatusesToExclude),
                    nin("transactionInfo.details.operationResult", npgStatusesToExclude)
                )
            ),
            sort(Document("insertionDate", -1)),
            skip(skip),
            limit(limit)
        )

        return Flux.from(getCollection().aggregate(pipeline))
    }

    /*@Aggregation(
        "{\$sort: {'insertionDate': -1}}",
        "{\$skip: ?0}",
        "{\$limit: ?1}",
    )
    fun findDeadLetterEventPaginatedOrderByInsertionDateDesc(
        skip: Int,
        limit: Int,
    ): Flux<DeadLetterEvent>*/

    fun findDeadLetterEventPaginatedOrderByInsertionDateDesc(
        skip: Int,
        limit: Int
    ): Flux<DeadLetterEvent> {
      
        val pipeline = listOf(
            sort(Document("insertionDate", -1)),
            skip(skip),
            limit(limit)
        )

        return Flux.from(getCollection().aggregate(pipeline))
    }

   /* @Aggregation(
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
    ): Flux<DeadLetterEvent>*/

    fun findDeadLetterEventPaginatedOrderByInsertionDateDescInTimeRangeWithExcludeStatuses(
        skip: Int,
        limit: Int,
        startTime: String,
        endTime: String,
        ecommerceStatusesToExclude: Set<String>,
        npgStatusesToExclude: Set<String>
    ): Flux<DeadLetterEvent> {
      
        val pipeline = listOf(
            match(
                and(
                    gte("insertionDate", startTime),
                    lte("insertionDate", endTime),
                    nin("transactionInfo.eCommerceStatus", ecommerceStatusesToExclude),
                    nin("transactionInfo.details.operationResult", npgStatusesToExclude)
                )
            ),
            sort(Document("insertionDate", -1)),
            skip(skip),
            limit(limit)
        )

        return Flux.from(getCollection().aggregate(pipeline))
    }

    private fun parseDate(dateStr: String): Instant {
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(dateStr))
    }

    fun count(): Mono<Long> {

        return Mono.from(getCollection().countDocuments())
    }



}
