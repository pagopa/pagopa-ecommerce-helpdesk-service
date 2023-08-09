package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.utils.resultToTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class PMTransactionDataProvider(@Autowired private val connectionFactory: ConnectionFactory) :
    TransactionDataProvider {

    override fun totalRecorcCount(searchCriteria: HelpDeskSearchTransactionRequestDto): Mono<Int> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> Mono.just(0)
            is SearchTransactionRequestRptIdDto -> Mono.just(0)
            is SearchTransactionRequestTransactionIdDto -> Mono.just(0)
            is SearchTransactionRequestEmailDto -> Mono.just(0) // TODO implementare
            is SearchTransactionRequestFiscalCodeDto -> Mono.just(0) // TODO implementare
            else ->
                Mono.error(
                    RuntimeException("Unhandled search criteria ${searchCriteria.javaClass}")
                )
        }

    override fun findResult(
        searchCriteria: HelpDeskSearchTransactionRequestDto,
        offset: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestRptIdDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestTransactionIdDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestEmailDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            is SearchTransactionRequestFiscalCodeDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            else ->
                Mono.error(
                    RuntimeException("Unhandled search criteria ${searchCriteria.javaClass}")
                )
        }


    private fun getTotalResultCount(
        totalRecordCountQuery: String
    ): Mono<Int> =
        Flux.usingWhen(
            connectionFactory.create(),
            { connection ->
                Flux.from(connection.createStatement(totalRecordCountQuery).execute())
                    .flatMap { result ->
                        result.map { row -> row[0, Integer::class.java]!!.toInt() }
                    }
                    .doOnNext { OraclePaginatedQueryLogger.logger.info("Total transaction found: $it") }
            },
            { it.close() }
        )
            .toMono()


    fun getResultSetFromPaginatedQuery(
        totalRecordCountQuery: String,
        resultQuery: String,
        pageNumber: Int,
        pageSize: Int
    ): Mono<Pair<Int, List<TransactionResultDto>>> =
        Flux.usingWhen(
            connectionFactory.create(),
            { connection ->
                val offset = (pageNumber + 1) * pageSize
                val query = resultQuery.format(offset, pageSize)
                OraclePaginatedQueryLogger.logger.info("Retrieving transactions for offset: $offset, limit: $pageSize.")

                Flux.from(connection.createStatement(query).execute()).flatMap {
                    resultToTransactionInfoDto(it)
                }
            },
            { it.close() }
        )
            .collectList()
            .flatMap { results ->
                if (results.isEmpty()) {
                    Mono.error(RuntimeException("No result found"))
                } else {
                    Flux.usingWhen(
                        connectionFactory.create(),
                        { connection ->
                            Flux.from(connection.createStatement(totalRecordCountQuery).execute())
                                .flatMap { result ->
                                    result.map { row -> row[0, Integer::class.java] }
                                }
                                .doOnNext { OraclePaginatedQueryLogger.logger.info("Total transaction found: $it") }
                        },
                        { it.close() }
                    )
                        .toMono()
                        .map { Pair(it.toInt(), results) }
                }
            }

}
