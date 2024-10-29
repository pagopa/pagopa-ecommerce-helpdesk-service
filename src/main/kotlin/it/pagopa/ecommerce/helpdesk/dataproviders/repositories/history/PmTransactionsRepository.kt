package it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history

import it.pagopa.ecommerce.helpdesk.documents.PmTransactionHistory
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PmTransactionsRepository : ReactiveCrudRepository<PmTransactionHistory, String> {

    @Query("{'userInfo.notificationEmail': '?0'}", count = true)
    fun countTransactionsWithEmail(email: String): Mono<Long>

    @Query("{'userInfo.notificationEmail': '?0'}", count = true)
    fun countTransactionsWithUserFiscalCode(userFiscalCode: String): Mono<Long>

    @Aggregation(
        "{\$match: {'userInfo.notificationEmail': '?0'}}",
        "{\$sort: {'transactionInfo.creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
        email: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransactionHistory>

    @Aggregation(
        "{\$match: {'userInfo.userFiscalCode': '?0'}}",
        "{\$sort: {'transactionInfo.creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
        userFiscalCode: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransactionHistory>
}
