package it.pagopa.ecommerce.helpdesk.dataproviders

import reactor.core.publisher.Mono

/**
 * Generic data provider interface. This interface models common function for transaction data
 * provider in order to search for transactions given a specific key criteria
 */
interface DataProvider<K, T> {

    /** Retrieve total record count for the given search parameters */
    fun totalRecordCount(searchParams: K): Mono<Int>

    /**
     * Perform paginated query for retrieve transaction information for the given search criteria
     */
    fun findResult(searchParams: K, skip: Int, limit: Int): Mono<List<T>>
}
