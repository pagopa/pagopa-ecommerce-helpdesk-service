package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
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

    override fun totalRecordCount(searchParams: HelpDeskSearchTransactionRequestDto): Mono<Int> {
        val searchCriteriaType = searchParams.type
        val invalidSearchCriteriaError =
            Mono.error<Int>(InvalidSearchCriteriaException(searchCriteriaType, ProductDto.PM))
        return when (searchParams) {
            is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
            is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
            is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
            is SearchTransactionRequestEmailDto ->
                getTotalResultCount(userEmailCountQuery, searchParams.userEmail)
            is SearchTransactionRequestFiscalCodeDto ->
                getTotalResultCount(
                    totalRecordCountQuery = userFiscalCodeCountQuery,
                    searchParam = searchParams.userFiscalCode
                )
            else -> Mono.error(InvalidSearchCriteriaException(searchParams.type, ProductDto.PM))
        }
    }

    override fun findResult(
        searchParams: HelpDeskSearchTransactionRequestDto,
        pageSize: Int,
        pageNumber: Int
    ): Mono<List<TransactionResultDto>> {
        val searchCriteriaType = searchParams.type
        val invalidSearchCriteriaError =
            Mono.error<List<TransactionResultDto>>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.PM)
            )
        return when (searchParams) {
            is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
            is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
            is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
            is SearchTransactionRequestEmailDto ->
                getResultSetFromPaginatedQuery(
                    resultQuery = userEmailPaginatedQuery,
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchParam = searchParams.userEmail,
                    searchType = searchCriteriaType
                )
            is SearchTransactionRequestFiscalCodeDto ->
                getResultSetFromPaginatedQuery(
                    resultQuery = userFiscalCodePaginatedQuery,
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchParam = searchParams.userFiscalCode,
                    searchType = searchCriteriaType
                )
            else -> invalidSearchCriteriaError
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
        pageNumber: Int,
        pageSize: Int,
        searchParam: String,
        searchType: String
    ): Mono<List<TransactionResultDto>> =
        Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    val offset = pageNumber * pageSize
                    logger.info("Retrieving transactions for offset: $offset, limit: $pageSize.")

                    Flux.from(
                            connection
                                .createStatement(resultQuery)
                                .bind(0, searchParam)
                                .bind(1, offset)
                                .bind(2, pageSize)
                                .execute()
                        )
                        .flatMap { resultToTransactionInfoDto(it) }
                },
                { it.close() }
            )
            .collectList()
            .switchIfEmpty { Mono.error(NoResultFoundException(searchType)) }
}
