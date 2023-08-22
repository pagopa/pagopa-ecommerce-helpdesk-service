package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.v1.Transaction
import org.springframework.data.domain.Pageable
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

    @Query("{'paymentNotices.paymentToken': '?0'}")
    fun findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
        paymentToken: String,
        pagination: Pageable
    ): Flux<Transaction>

    @Query("{'paymentNotices.rptId': '?0'}", count = true)
    fun countTransactionsWithRptId(rpiId: String): Mono<Long>

    @Query("{'paymentNotices.rptId': '?0'}")
    fun findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
        rptId: String,
        pagination: Pageable
    ): Flux<Transaction>
}
