package it.pagopa.ecommerce.helpdesk

import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestEmailDto
import reactor.core.publisher.Mono

/** Data class that wraps search parameter with optional ConfidentialMailUtils class */
class SearchParamDecoder<out T>(
    val searchParameter: T,
    val confidentialMailUtils: ConfidentialMailUtils?
) where T : HelpDeskSearchTransactionRequestDto {

    fun decode(): Mono<out HelpDeskSearchTransactionRequestDto> =
        when (searchParameter) {
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
