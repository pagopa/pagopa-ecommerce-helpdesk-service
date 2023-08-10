package it.pagopa.ecommerce.helpdesk.dataproviders

import com.fasterxml.jackson.annotation.JsonSubTypes
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import reactor.core.publisher.Mono

/**
 * Transaction data provider interface. This interface models common function for transaction data
 * provider in order to search for transactions given a specific HelpDeskSearchTransactionRequestDto
 * criteria
 *
 * @see HelpDeskSearchTransactionRequestDto
 */
interface TransactionDataProvider {

    /** Retrieve total record count for the given search criteria */
    fun totalRecordCount(searchCriteria: HelpDeskSearchTransactionRequestDto): Mono<Long>

    /**
     * Perform paginated query for retrieve transaction information for the given search criteria
     */
    fun findResult(
        searchCriteria: HelpDeskSearchTransactionRequestDto,
        pageSize: Int,
        pageNumber: Int
    ): Mono<List<TransactionResultDto>>

    object SearchTypeMapping {
        private val mapping = mutableMapOf<Class<*>, String>()
        fun getSearchType(clazz: Class<*>): String {
            if (mapping.isEmpty()) {
                HelpDeskSearchTransactionRequestDto::class
                    .java
                    .getAnnotationsByType(JsonSubTypes::class.java)
                    .forEach {
                        it.value.forEach { jsonSubType ->
                            mapping[jsonSubType.value.java] = jsonSubType.name
                        }
                    }
            }
            return mapping[clazz] ?: "UNKNOWN"
        }
    }
}
