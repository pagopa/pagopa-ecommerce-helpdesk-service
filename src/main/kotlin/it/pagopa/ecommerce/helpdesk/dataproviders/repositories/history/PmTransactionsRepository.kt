package it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history

import com.mongodb.reactivestreams.client.MongoClient
import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import it.pagopa.ecommerce.helpdesk.configurations.MongoClientsProvider
import it.pagopa.ecommerce.helpdesk.documents.PmTransactionHistory
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Aggregates.*
import io.smallrye.mutiny.Uni
import org.bson.Document
import org.reactivestreams.FlowAdapters
import org.reactivestreams.Publisher
import java.util.concurrent.Flow


@ApplicationScoped
class PmTransactionsRepository  {

    @Inject
    lateinit var mongoClient: MongoClient

    @Inject
    lateinit var mongoClientsProvider: MongoClientsProvider

    private val collectionName = " pm-transactions-view"

    fun <T> convertToFlowPublisher(publisher: Publisher<T>): Flow.Publisher<T> {
        return FlowAdapters.toFlowPublisher(publisher)
    }


    private fun getCollection() =
        mongoClient.getDatabase(mongoClientsProvider.getDefaultDatabaseName())
            .getCollection(collectionName, PmTransactionHistory::class.java)


    /*@Query("{'userInfo.notificationEmail': '?0'}", count = true)
    fun countTransactionsWithEmail(email: String): Mono<Long>*/

    fun countTransactionsWithEmail(email: String): Uni<Long> {
        val filter = eq("userInfo.notificationEmail", email)
        val publisher: Publisher<Long> = getCollection().countDocuments(filter)
        val flowPublisher: Flow.Publisher<Long> = FlowAdapters.toFlowPublisher(publisher)
        return Uni.createFrom().publisher(flowPublisher)
    }

    /*@Query("{'userInfo.userFiscalCode': '?0'}", count = true)
    fun countTransactionsWithUserFiscalCode(userFiscalCode: String): Mono<Long>*/

    fun countTransactionsWithUserFiscalCode(userFiscalCode: String): Uni<Long> {
        val filter = eq("userInfo.userFiscalCode", userFiscalCode)
        val publisher: Publisher<Long> = getCollection().countDocuments(filter)
        val flowPublisher: Flow.Publisher<Long> = FlowAdapters.toFlowPublisher(publisher)
        return Uni.createFrom().publisher(flowPublisher)
    }

    /*@Aggregation(
        "{\$match: {'userInfo.notificationEmail': '?0'}}",
        "{\$sort: {'transactionInfo.creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
        email: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransactionHistory>*/

    fun findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
        email: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransactionHistory> {
        val pipeline = listOf(
            match(eq("userInfo.notificationEmail", email)),
            sort(Document("transactionInfo.creationDate", -1)),
            skip(skip),
            limit(limit)
        )
        return Flux.from(getCollection().aggregate(pipeline, PmTransactionHistory::class.java))
    }

    /*@Aggregation(
        "{\$match: {'userInfo.userFiscalCode': '?0'}}",
        "{\$sort: {'transactionInfo.creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
        userFiscalCode: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransactionHistory>*/
    fun findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
        userFiscalCode: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransactionHistory> {
        val pipeline = listOf(
            match(eq("userInfo.userFiscalCode", userFiscalCode)),
            sort(Document("transactionInfo.creationDate", -1)),
            skip(skip),
            limit(limit)
        )
        return Flux.from(getCollection().aggregate(pipeline, PmTransactionHistory::class.java))
    }
}
