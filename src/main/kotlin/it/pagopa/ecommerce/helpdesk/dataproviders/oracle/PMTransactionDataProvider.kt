package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.resultToTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

/**
 * TransactionDataProvider implementation that search transactions into PM DB
 *
 * @see TransactionDataProvider
 */
@Component
class PMTransactionDataProvider(@Autowired private val connectionFactory: ConnectionFactory) :
    TransactionDataProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun totalRecordCount(
        searchParams: SearchParamDecoder<HelpDeskSearchTransactionRequestDto>
    ): Mono<Int> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMap {
                Mono.error<Int>(InvalidSearchCriteriaException(it.type, ProductDto.PM))
            }
        return decodedSearchParam.flatMap {
            when (it) {
                is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
                is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
                is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
                is SearchTransactionRequestEmailDto ->
                    getTotalResultCount(userEmailCountQuery, it.userEmail)
                is SearchTransactionRequestFiscalCodeDto ->
                    getTotalResultCount(
                        totalRecordCountQuery = userFiscalCodeCountQuery,
                        searchParam = it.userFiscalCode
                    )
                else -> invalidSearchCriteriaError
            }
        }
    }

    override fun findResult(
        searchParams: SearchParamDecoder<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMap {
                Mono.error<List<TransactionResultDto>>(
                    InvalidSearchCriteriaException(it.type, ProductDto.PM)
                )
            }
        return decodedSearchParam.flatMap {
            when (it) {
                is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
                is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
                is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
                is SearchTransactionRequestEmailDto ->
                    getResultSetFromPaginatedQuery(
                        resultQuery = userEmailPaginatedQuery,
                        skip = skip,
                        limit = limit,
                        searchParam = it.userEmail,
                        searchType = it.type
                    )
                is SearchTransactionRequestFiscalCodeDto ->
                    getResultSetFromPaginatedQuery(
                        resultQuery = userFiscalCodePaginatedQuery,
                        skip = skip,
                        limit = limit,
                        searchParam = it.userFiscalCode,
                        searchType = it.type
                    )
                else -> invalidSearchCriteriaError
            }
        }
    }

    private fun getTotalResultCount(totalRecordCountQuery: String, searchParam: String): Mono<Int> =
        Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    Flux.from(
                            connection
                                .createStatement(totalRecordCountQuery)
                                .bind(0, searchParam)
                                .execute()
                        )
                        .flatMap { result ->
                            result.map { row -> row[0, java.lang.Long::class.java]!!.toInt() }
                        }
                        .doOnNext { logger.info("Total transaction found: $it") }
                },
                { it.close() }
            )
            .toMono()

    private fun getResultSetFromPaginatedQuery(
        resultQuery: String,
        skip: Int,
        limit: Int,
        searchParam: String,
        searchType: String
    ): Mono<List<TransactionResultDto>> =
        Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    logger.info(
                        "Retrieving transactions from PM database. Skipping: $skip, limit: $limit."
                    )

                    Flux.from(
                            connection
                                .createStatement(resultQuery)
                                .bind(0, searchParam)
                                .bind(1, skip)
                                .bind(2, limit)
                                .execute()
                        )
                        .flatMap { resultToTransactionInfoDto(it) }
                },
                { it.close() }
            )
            .collectList()
            .switchIfEmpty { Mono.error(NoResultFoundException(searchType)) }
}
