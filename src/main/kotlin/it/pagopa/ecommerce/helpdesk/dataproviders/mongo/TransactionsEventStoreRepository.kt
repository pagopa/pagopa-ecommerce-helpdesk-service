package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

/** eCommerce Mongo event store repository */
@Repository
interface TransactionsEventStoreRepository<T> :
    ReactiveCrudRepository<BaseTransactionEvent<T>, String> {
    fun findByTransactionIdOrderByCreationDateAsc(
        idTransaction: String
    ): Flux<BaseTransactionEvent<T>>
}
