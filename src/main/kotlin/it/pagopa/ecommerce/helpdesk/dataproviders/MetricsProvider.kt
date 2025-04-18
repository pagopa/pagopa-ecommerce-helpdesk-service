package it.pagopa.ecommerce.helpdesk.dataproviders

import reactor.core.publisher.Mono

/**
 * Generic metrics provider interface. This interface models common function for metrics data
 * provider in order to compute metrics by key criteria
 */
interface MetricsProvider<K, T> {

    /** Retrieve metrics for the given search parameters */
    fun computeMetrics(criteria: K): Mono<T>
}
