package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.OraclePaginatedQueryLogger.logger
import it.pagopa.ecommerce.helpdesk.utils.resultToTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

object OraclePaginatedQueryLogger {
    val logger: Logger = LoggerFactory.getLogger(OraclePaginatedQueryLogger::class.java)
}

fun getResultSetFromPaginatedQuery(
    connectionFactory: ConnectionFactory,
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
                logger.info(
                    "Retrieving transactions for offset: $offset, limit: $pageSize. Query: $query"
                ) // TODO remove query log

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
                                .doOnNext { logger.info("Total transaction found: $it") }
                        },
                        { it.close() }
                    )
                    .toMono()
                    .map { Pair(it.toInt(), results) }
            }
        }
