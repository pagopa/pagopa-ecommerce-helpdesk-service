package it.pagopa.ecommerce.helpdesk.dataproviders.v1

import it.pagopa.ecommerce.helpdesk.dataproviders.DataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchBulkTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestTransactionIdRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionBulkResultDto
import reactor.core.publisher.Mono

/**
 * Transaction data provider interface. This interface models common function for transaction data
 * provider in order to search for bulk transactions given a specific
 * SearchTransactionRequestTransactionIdRangeDto criteria
 *
 * @see SearchTransactionRequestTransactionIdRangeDto
 * @see DataProvider
 * @see TransactionBulkResultDto
 */
interface BulkTransactionDataProvider {

    /**
     * Perform query for retrieve bulk transaction information for the given search range criteria
     */
    fun findResult(
        searchParams: PmSearchBulkTransactionRequestDto,
    ): Mono<List<TransactionBulkResultDto>>
}
