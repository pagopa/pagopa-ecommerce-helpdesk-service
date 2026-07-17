package it.pagopa.ecommerce.helpdesk.dataproviders

import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import reactor.core.publisher.Flux

object Utils {

    fun readEventsFromDbs(
        onlineDbQuery: (skip: Int, limit: Int) -> Flux<BaseTransactionView>,
        historyDbQuery: (skip: Int, limit: Int) -> Flux<BaseTransactionView>,
        skip: Int,
        limit: Int,
        countInfo: CountInfo
    ): Flux<BaseTransactionView> {
        val onlineCount = countInfo.onlineDbCount
        val historicalCount = countInfo.historyDbCount
        /*
         * we serve online db records first and then historyDb ones concatenated
         * this order reflect the fact that records are ordered by descending timestamp
         * and history db surely contains records older than online DB
         */
        val recordOffset = skip + limit
        return if (recordOffset <= onlineCount) {
            // in this case we have to serve db only records
            onlineDbQuery(skip, limit)
        } else {
            if (skip < onlineCount) {
                // in this case requested offset overlap between online and history db, we have to
                // retrieve records from both datasource
                val onlineDbLimit = onlineCount - skip
                val historyDbLimit = limit - onlineDbLimit
                Flux.concat(
                    onlineDbQuery(skip, onlineDbLimit.toInt()),
                    if (historicalCount > 0) {
                        historyDbQuery(0, historyDbLimit.toInt())
                    } else {
                        Flux.empty()
                    }
                )
            } else {
                // otherwise we have left historical db records only
                val historyDbSkip = skip - onlineCount
                historyDbQuery(historyDbSkip.toInt(), limit)
            }
        }
    }
}
