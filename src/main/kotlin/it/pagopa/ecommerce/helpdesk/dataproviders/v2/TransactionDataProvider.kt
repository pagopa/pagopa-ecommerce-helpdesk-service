package it.pagopa.ecommerce.helpdesk.dataproviders.v2

import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoderV2
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionResultDto
import reactor.core.publisher.Mono

/**
 * Transaction data provider interface. This interface models common function for transaction data
 * provider in order to search for transactions given a specific
 * EcommerceSearchTransactionRequestDto criteria
 *
 * @see EcommerceSearchTransactionRequestDto
 * @see DataProvider
 * @see TransactionResultDto
 */
interface TransactionDataProvider :
    DataProvider<SearchParamDecoderV2<EcommerceSearchTransactionRequestDto>, TransactionResultDto> {

    /** Retrieve total record count for the given search parameters */
    override fun totalRecordCount(
        searchParams: SearchParamDecoderV2<EcommerceSearchTransactionRequestDto>
    ): Mono<Int>

    /**
     * Perform paginated query for retrieve transaction information for the given search criteria
     */
    override fun findResult(
        searchParams: SearchParamDecoderV2<EcommerceSearchTransactionRequestDto>,
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>>
}
