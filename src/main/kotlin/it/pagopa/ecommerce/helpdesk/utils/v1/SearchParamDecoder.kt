package it.pagopa.ecommerce.helpdesk.utils.v1

import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestEmailDto
import reactor.core.publisher.Mono

/** Data class that wraps search parameter with optional ConfidentialMailUtils class */
class SearchParamDecoder<out T>(
    val searchParameter: T,
    val confidentialMailUtils: ConfidentialMailUtils?
) where T : HelpDeskSearchTransactionRequestDto {

    /**
     * Decode search parameter to the one to be used for transaction searching.
     *
     * @return the decoded search parameter if a decoding operation is defined for input parameter
     *   or search parameter itself without any transformation
     */
    fun decode(): Mono<out HelpDeskSearchTransactionRequestDto> =
        when (searchParameter) {
            /*
             * Search parameter decoding is performed iff input search parameter is an email and the searching is performed for eCommerce transactions.
             * In fact confidentialMailUtils parameter is optional and valued only when searching for eCommerce transactions
             */
            is SearchTransactionRequestEmailDto ->
                if (confidentialMailUtils != null) {
                    confidentialMailUtils.toConfidential(searchParameter.userEmail).map {
                        SearchTransactionRequestEmailDto().userEmail(it.opaqueData).type("EMAIL")
                    }
                } else {
                    Mono.just(searchParameter)
                }
            else -> Mono.just(searchParameter)
        }
}
