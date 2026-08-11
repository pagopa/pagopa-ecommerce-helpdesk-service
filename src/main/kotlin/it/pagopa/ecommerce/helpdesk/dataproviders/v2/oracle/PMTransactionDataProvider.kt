package it.pagopa.ecommerce.helpdesk.dataproviders.v2.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.CountInfo
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.resultToTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
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
@Component("PMTransactionDataProviderV2")
class PMTransactionDataProvider(@Autowired private val connectionFactory: ConnectionFactory) :
    TransactionDataProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun totalRecordCount(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>
    ): Mono<CountInfo> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMap {
                Mono.error<Int>(InvalidSearchCriteriaException(it.type, ProductDto.PM))
            }
        return decodedSearchParam
            .flatMap {
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
            .map { CountInfo(it.toLong(), 0) }
    }

    override fun findResult(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int,
        countInfo: CountInfo
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
                        .doOnNext {
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(mapOf("total" to it.toString()))
                                .dependency("PM-db")
                                .logInfo(logger, "Total transaction found")
                        }
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
                    Flux.from(
                            connection
                                .createStatement(resultQuery)
                                .bind(0, searchParam)
                                .bind(1, skip)
                                .bind(2, limit)
                                .execute()
                        )
                        .flatMap { resultToTransactionInfoDto(it) }
                        .doOnNext {
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf("skip" to skip.toString(), "limit" to limit.toString())
                                )
                                .dependency("PM-db")
                                .logInfo(logger, "Transactions from PM database retrieved")
                        }
                },
                { it.close() }
            )
            .collectList()
            .switchIfEmpty { Mono.error(NoResultFoundException(searchType)) }
}
