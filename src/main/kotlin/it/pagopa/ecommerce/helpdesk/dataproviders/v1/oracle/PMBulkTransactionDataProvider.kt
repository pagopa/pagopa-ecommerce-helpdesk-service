package it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.BulkTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.LogTracingTags
import it.pagopa.ecommerce.helpdesk.utils.v1.resultToBulkTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchBulkTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestTransactionIdRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionBulkResultDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * PMBulkTransactionDataProvider implementation that searches bulk transactions in PM DB
 *
 * @see BulkTransactionDataProvider
 */
@Component
class PMBulkTransactionDataProvider(@Autowired private val connectionFactory: ConnectionFactory) :
    BulkTransactionDataProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findResult(
        searchParams: PmSearchBulkTransactionRequestDto
    ): Mono<List<TransactionBulkResultDto>> {

        val invalidSearchCriteriaError =
            Mono.error<List<TransactionBulkResultDto>>(
                InvalidSearchCriteriaException(searchParams.type, ProductDto.PM)
            )

        return when (searchParams) {
            is SearchTransactionRequestTransactionIdRangeDto -> {
                getResultSetFromTransactionIdRangeQuery(
                    resultQuery = transactionIdRangeQuery,
                    type = searchParams.type,
                    startTransactionId = searchParams.transactionIdRange.startTransactionId,
                    endTransactionId = searchParams.transactionIdRange.endTransactionId
                )
            }
            else -> invalidSearchCriteriaError
        }
    }

    /** Retrieves transaction results based on a range of transaction IDs. */
    private fun getResultSetFromTransactionIdRangeQuery(
        resultQuery: String,
        type: String,
        startTransactionId: String,
        endTransactionId: String
    ): Mono<List<TransactionBulkResultDto>> {

        return Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    Flux.from(
                            connection
                                .createStatement(resultQuery)
                                .apply {
                                    bind(0, startTransactionId.toLong())
                                    bind(1, endTransactionId.toLong())
                                }
                                .execute()
                        )
                        .flatMap { result ->
                            if (logger.isDebugEnabled) {
                                LogTracingUtils.loggerTracingUtils()
                                    .success()
                                    .logDebug(
                                        logger,
                                        "Query executed successfully. Processing results."
                                    )
                            }
                            resultToBulkTransactionInfoDto(result)
                        }
                        .doOnComplete {
                            LogTracingUtils.loggerTracingUtils()
                                .success()
                                .details(
                                    mapOf(
                                        "type" to type,
                                        "start_transaction_id" to startTransactionId,
                                        "end_transaction_id" to endTransactionId
                                    )
                                )
                                .dependency(LogTracingTags.Dependency.PM_DB)
                                .logInfo(
                                    logger,
                                    "Transactions from PM database from a given transactionId range processed"
                                )
                        }
                },
                { connection ->
                    if (logger.isDebugEnabled) {
                        LogTracingUtils.loggerTracingUtils()
                            .success()
                            .logDebug(logger, "Closing connection")
                    }
                    connection.close()
                }
            )
            .collectList()
            .flatMap { results ->
                if (results.isEmpty()) {
                    LogTracingUtils.loggerTracingUtils()
                        .failure()
                        .details(
                            mapOf(
                                "type" to type,
                                "start_transaction_id" to startTransactionId,
                                "end_transaction_id" to endTransactionId
                            )
                        )
                        .dependency(LogTracingTags.Dependency.PM_DB)
                        .logWarn(logger, "No results found for a given transactionId range")
                    Mono.error(NoResultFoundException(type))
                } else {
                    Mono.just(results)
                }
            }
    }
}
