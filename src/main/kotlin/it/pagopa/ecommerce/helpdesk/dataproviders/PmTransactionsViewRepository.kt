package it.pagopa.ecommerce.helpdesk.dataproviders

import it.pagopa.ecommerce.helpdesk.documents.PmTransaction
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface PmTransactionsViewRepository: ReactiveCrudRepository<PmTransaction , String> {

    @Query("{'email.data': '?0'}", count = true)
    fun countTransactionsWithEmail(email: String): Mono<Long>

    @Query("{'fiscalcode.data': '?0'}", count = true)
    fun countTransactionsWithUserFiscalCode(userFiscalCode: String): Mono<Long>

    @Aggregation(
        "{\$match: {'email.data': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
        email: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransaction>

    @Aggregation(
        "{\$match: {'email.data': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
        userFiscalCode: String,
        skip: Int,
        limit: Int
    ): Flux<PmTransaction>


}