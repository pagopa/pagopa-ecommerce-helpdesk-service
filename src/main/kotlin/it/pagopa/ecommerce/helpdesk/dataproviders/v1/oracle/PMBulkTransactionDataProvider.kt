package it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.BulkTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.v1.resultToBulkTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

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
    ): Flux<TransactionBulkResultDto> {

        val invalidSearchCriteriaError =
            Flux.error<TransactionBulkResultDto>(
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
        endTransactionId: String,
    ): Flux<TransactionBulkResultDto> {
        return Flux.usingWhen(
                connectionFactory.create(),
                { connection ->
                    logger.info(
                        "Retrieving transactions from PM database given transactionId range [$startTransactionId, $endTransactionId]"
                    )
                    Flux.from(
                            connection
                                .createStatement(resultQuery)
                                .apply {
                                    bind(0, startTransactionId)
                                    bind(1, endTransactionId)
                                }
                                .execute()
                        )
                        .flatMap { result ->
                            logger.debug("Query executed successfully. Processing results.")
                            resultToBulkTransactionInfoDto(result)
                        }
                },
                { connection ->
                    logger.debug("Closing connection.")
                    connection.close()
                }
            )
            .switchIfEmpty {
                logger.warn(
                    "No results found for transactionId range [$startTransactionId, $endTransactionId]."
                )
                Flux.error<TransactionBulkResultDto>(NoResultFoundException(type))
            }
    }
}
