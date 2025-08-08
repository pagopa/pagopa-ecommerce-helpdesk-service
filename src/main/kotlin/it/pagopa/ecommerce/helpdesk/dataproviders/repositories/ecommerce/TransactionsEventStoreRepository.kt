package it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce

import com.mongodb.reactivestreams.client.MongoClient
import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.helpdesk.configurations.MongoClientsProvider
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts.ascending

/** eCommerce Mongo event store repository */
@ApplicationScoped
class TransactionsEventStoreRepository<T>{

    @Inject
    lateinit var mongoClient: MongoClient

    @Inject
    lateinit var mongoClientsProvider: MongoClientsProvider

    private val collectionName = "eventstore"
   /* fun findByTransactionIdOrderByCreationDateAsc(
        idTransaction: String
    ): Flux<BaseTransactionEvent<T>>*/

    @Suppress("UNCHECKED_CAST")
    private fun getCollection(): com.mongodb.reactivestreams.client.MongoCollection<BaseTransactionEvent<T>> =
        mongoClient.getDatabase(mongoClientsProvider.getDefaultDatabaseName())
            .getCollection(collectionName, BaseTransactionEvent::class.java) as com.mongodb.reactivestreams.client.MongoCollection<BaseTransactionEvent<T>>

    fun findByTransactionIdOrderByCreationDateAsc(transactionId: String): Flux<BaseTransactionEvent<T>> {
        val filter = eq("transactionId", transactionId)
        val sort = ascending("creationDate")

        return Flux.from(
            getCollection().find(filter).sort(sort)
        )
    }
}
