package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.v1.Transaction
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/** eCommerce Mongo transaction view repository */
@Repository
interface TransactionsViewRepository : ReactiveCrudRepository<Transaction, String> {

    @Query("{'paymentNotices.paymentToken': '?0'}", count = true)
    fun countTransactionsWithPaymentToken(paymentToken: String): Mono<Long>

    @Aggregation(
        "{\$match: {'paymentNotices.paymentToken': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
        paymentToken: String,
        skip: Int,
        limit: Int,
    ): Flux<Transaction>

    @Query("{'paymentNotices.rptId': '?0'}", count = true)
    fun countTransactionsWithRptId(rpiId: String): Mono<Long>

    @Aggregation(
        "{\$match: {'paymentNotices.rptId': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
        rptId: String,
        skip: Int,
        limit: Int
    ): Flux<Transaction>
}
