package it.pagopa.ecommerce.helpdesk.dataproviders

import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoder
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import reactor.core.publisher.Mono

/**
 * Transaction data provider interface. This interface models common function for transaction data
 * provider in order to search for transactions given a specific HelpDeskSearchTransactionRequestDto
 * criteria
 *
 * @see HelpDeskSearchTransactionRequestDto
 * @see DataProvider
 * @see TransactionResultDto
 */
interface TransactionDataProvider :
    DataProvider<SearchParamDecoder<HelpDeskSearchTransactionRequestDto>, TransactionResultDto> {

    /** Retrieve total record count for the given search parameters */
    override fun totalRecordCount(
        searchParams: SearchParamDecoder<HelpDeskSearchTransactionRequestDto>
    ): Mono<Int>

    /**
     * Perform paginated query for retrieve transaction information for the given search criteria
     */
    override fun findResult(
        searchParams: SearchParamDecoder<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>>
}
