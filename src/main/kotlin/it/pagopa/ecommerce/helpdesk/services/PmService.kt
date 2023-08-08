package it.pagopa.ecommerce.helpdesk.services

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.buildTransactionByUserEmailCountQuery
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.buildTransactionByUserEmailPaginatedQuery
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.util.stream.IntStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class PmService(@Autowired val connectionFactory: ConnectionFactory) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        pmSearchTransactionRequestDto: Mono<PmSearchTransactionRequestDto>
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk pm service] searchTransaction method")

        return pmSearchTransactionRequestDto
            .doOnNext { logger.info("Search type: ${it.type}") }
            .flatMap {
                Flux.usingWhen(
                        connectionFactory.create(),
                        { connection ->
                            Flux.from(
                                    connection
                                        .createStatement(
                                            buildTransactionByUserEmailCountQuery(
                                                "marco.tormen@pagopa.it"
                                            )
                                        )
                                        .execute()
                                )
                                .flatMap { result ->
                                    result.map { row -> row[0, Integer::class.java] }
                                }
                                .toMono()
                                .flatMapMany { totalCount ->
                                    logger.info("Total records found: $totalCount")
                                    val offset = (pageNumber + 1) * pageSize
                                    logger.info(
                                        "Retrieving transactions for offset: $offset, limit: $pageSize"
                                    )
                                    Flux.from(
                                            connection
                                                .createStatement(
                                                    buildTransactionByUserEmailPaginatedQuery(
                                                        userEmail = "email@email.it",
                                                        limit = pageSize,
                                                        offset = offset
                                                    )
                                                )
                                                .execute()
                                        )
                                        .flatMap { result ->
                                            result.map { row ->
                                                val stringBuilder = StringBuilder()
                                                IntStream.range(0, 22).forEach {
                                                    stringBuilder.append(
                                                        "Column $it, value ${row[it, String::class.java]}\n"
                                                    )
                                                }
                                                stringBuilder
                                            }
                                        }
                                        .doOnNext { logger.info("Row read from DB: $it") }
                                }
                        },
                        { it.close() }
                    )
                    .toMono()
            }
            .map { SearchTransactionResponseDto() }
    }

    fun searchTransactionByUserEmail(userEmail: String) {}
}
