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

    override fun totalRecordCount(searchCriteria: HelpDeskSearchTransactionRequestDto): Mono<Int> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> Mono.just(0)
            is SearchTransactionRequestRptIdDto -> Mono.just(0)
            is SearchTransactionRequestTransactionIdDto -> Mono.just(0)
            is SearchTransactionRequestEmailDto ->
                getTotalResultCount(buildTransactionByUserEmailCountQuery(searchCriteria.userEmail))
            is SearchTransactionRequestFiscalCodeDto ->
                getTotalResultCount(
                    buildTransactionByUserFiscalCodeCountQuery(searchCriteria.userFiscalCode)
                )
            else ->
                Mono.error(
                    InvalidSearchCriteriaException(
                        TransactionDataProvider.SearchTypeMapping.getSearchType(
                            searchCriteria.javaClass
                        ),
                        ProductDto.PM
                    )
                )
        }

    override fun findResult(
        searchCriteria: HelpDeskSearchTransactionRequestDto,
        pageSize: Int,
        pageNumber: Int
    ): Mono<List<TransactionResultDto>> {
        val searchCriteriaType =
            TransactionDataProvider.SearchTypeMapping.getSearchType(searchCriteria.javaClass)
        val invalidSearchCriteriaError =
            Mono.error<List<TransactionResultDto>>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.PM)
            )
        return when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
            is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
            is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
            is SearchTransactionRequestEmailDto ->
                getResultSetFromPaginatedQuery(
                    resultQuery =
                        buildTransactionByUserEmailPaginatedQuery(searchCriteria.userEmail),
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchType = searchCriteriaType
                )
            is SearchTransactionRequestFiscalCodeDto ->
                getResultSetFromPaginatedQuery(
                    resultQuery =
                        buildTransactionByUserFiscalCodePaginatedQuery(
                            searchCriteria.userFiscalCode
                        ),
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchType = searchCriteriaType
                )
            else -> invalidSearchCriteriaError
        }
    }

    private fun getTotalResultCount(totalRecordCountQuery: String): Mono<Int> =
        Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    Flux.from(connection.createStatement(totalRecordCountQuery).execute())
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
        searchType: String
    ): Mono<List<TransactionResultDto>> =
        Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    val offset = pageNumber * pageSize
                    val query = resultQuery.format(offset, pageSize)
                    logger.info("Retrieving transactions for offset: $offset, limit: $pageSize.")

                    Flux.from(connection.createStatement(query).execute()).flatMap {
                        resultToTransactionInfoDto(it)
                    }
                },
                { it.close() }
            )
            .collectList()
            .switchIfEmpty { Mono.error(NoResultFoundException(searchType)) }
}
